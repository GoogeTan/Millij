package katze.millij.toolwindow

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.util.ExecUtil
import com.intellij.execution.{ProgramRunnerUtil, RunManager}
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBPanel, JBScrollPane}
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.{DoubleClickListener, SearchTextField}
import katze.millij.configuration.{MillRunConfiguration, MillRunConfigurationType}

import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.tree.{DefaultMutableTreeNode, DefaultTreeModel}

class MillModulesToolWindowFactory extends ToolWindowFactory:
  private var tasks: List[String] = List.empty
  lazy val icon = IconLoader.getIcon("/icons/millFile.svg", classOf[MillModulesToolWindowFactory])

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit =
    toolWindow.setIcon(icon)
    val tree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Loading Mill Tasks...")))
    tree.setRootVisible(true)

    val actionGroup = new DefaultActionGroup()
    val reloadAction = new AnAction("Reload", "Reload Mill tasks", AllIcons.Actions.Refresh):
      override def actionPerformed(e: AnActionEvent): Unit =
        loadTasksAsync(project, tree)
    actionGroup.add(reloadAction)

    val topPanel = new JBPanel(new BorderLayout())
    val toolbar: ActionToolbar = ActionManager.getInstance().createActionToolbar("MillToolbar", actionGroup, true)
    toolbar.setTargetComponent(topPanel)
    topPanel.add(toolbar.getComponent, BorderLayout.WEST)
    topPanel.add(makeSearchField(newRoot => tree.setModel(new DefaultTreeModel(newRoot))), BorderLayout.CENTER)

    val contentPanel = new JBPanel(new BorderLayout())
    contentPanel.add(topPanel, BorderLayout.NORTH)
    contentPanel.add(new JBScrollPane(tree), BorderLayout.CENTER)

    val panel = new SimpleToolWindowPanel(true, true)
    panel.setContent(contentPanel)
    val content = ContentFactory.getInstance().createContent(panel, null, false)
    toolWindow.getContentManager.addContent(content)

    loadTasksAsync(project, tree)

    new DoubleClickListener():
      override def onDoubleClick(event: MouseEvent): Boolean =
        val path = tree.getClosestPathForLocation(event.getX, event.getY)
        if path != null then
          val bounds = tree.getPathBounds(path)
          if bounds != null && event.getY >= bounds.y && event.getY < (bounds.y + bounds.height) then
            val node = path.getLastPathComponent.asInstanceOf[DefaultMutableTreeNode]
            if node.isLeaf && node.getParent != null then
              val taskName = getTaskPath(node)
              if taskName.nonEmpty then
                runMillTask(project, taskName)
                return true
        false
    .installOn(tree)
  end createToolWindowContent

  def makeSearchField(setTree : DefaultMutableTreeNode => Unit) =
    val searchField = new SearchTextField()
    searchField.addDocumentListener(new javax.swing.event.DocumentListener {
      override def insertUpdate(e: javax.swing.event.DocumentEvent): Unit = filterTree()

      override def removeUpdate(e: javax.swing.event.DocumentEvent): Unit = filterTree()

      override def changedUpdate(e: javax.swing.event.DocumentEvent): Unit = filterTree()

      def filterTree(): Unit = {
        val searchText = searchField.getText
        val filteredTasks = tasks.filter(_.contains(searchText))
        val newRoot = buildTree(filteredTasks)
        setTree(newRoot)
      }
    })
    searchField
  end makeSearchField

  private def getTaskPath(node: DefaultMutableTreeNode): String =
    val path = node.getPath.map(_.asInstanceOf[DefaultMutableTreeNode].getUserObject.toString)
    if path.length > 1 then // exclude the root node
      path.tail.mkString(".")
    else
      ""

  def executeMillCommand(projectPath: String, timeoutMs: Int = 15000): Either[String, List[String]] =
    val cmd = new GeneralCommandLine("mill", "--no-server", "resolve", "__")
    cmd.setWorkDirectory(projectPath)

    try
      val processOutput = ExecUtil.execAndGetOutput(cmd, timeoutMs)

      if processOutput.getExitCode == 0 then
        Right(processOutput.getStdout.split("\n").map(_.trim).filter(_.nonEmpty).toList)
      else
        Left(s"Mill failed: ${processOutput.getStderr}")
    catch
      case e: Exception =>
        Left(s"Command execution threw an exception: ${e.getMessage}")
  end executeMillCommand

  private def buildTree(tasks: List[String]): DefaultMutableTreeNode =
    val root = new DefaultMutableTreeNode("Mill Project")
    if tasks.isEmpty then
      root.add(new DefaultMutableTreeNode("No tasks found"))
    else
      tasks.foreach:
        task =>
          val parts = task.split("\\.")
          var currentNode = root
          for part <- parts do
            var found: DefaultMutableTreeNode = null
            for i <- 0 until currentNode.getChildCount do
              val child = currentNode.getChildAt(i).asInstanceOf[DefaultMutableTreeNode]
              if child.getUserObject == part then found = child
            if found == null then
              found = new DefaultMutableTreeNode(part)
              currentNode.add(found)
            currentNode = found
    root
  end buildTree

  def loadTasksAsync(project: Project, tree: Tree): Unit =
    tree.setPaintBusy(true)

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Resolving Mill Tasks", true):
      override def run(indicator: ProgressIndicator): Unit =
        val projectPath = project.getBasePath
        if projectPath == null then return

        val tasksResult = executeMillCommand(projectPath)

        val rootNode = tasksResult match
          case Right(newTasks) =>
            tasks = newTasks
            buildTree(tasks)
          case Left(error) =>
            val root = new DefaultMutableTreeNode("Error")
            error.split("\n").foreach(line => root.add(new DefaultMutableTreeNode(line)))
            root

        ApplicationManager.getApplication.invokeLater { () =>
          tree.setModel(new DefaultTreeModel(rootNode))
          tree.setPaintBusy(false)
        }
    )
  end loadTasksAsync

  private def runMillTask(project: Project, taskName: String): Unit =
    val runManager = RunManager.getInstance(project)
    val configurationType = new MillRunConfigurationType()
    val configurationFactory = configurationType.getConfigurationFactories.head
    val settings = runManager.createConfiguration(s"Mill: $taskName", configurationFactory)
    val configuration = settings.getConfiguration.asInstanceOf[MillRunConfiguration]
    configuration.taskName = taskName
    runManager.addConfiguration(settings)
    runManager.setSelectedConfiguration(settings)

    val executor = DefaultRunExecutor.getRunExecutorInstance
    val builder = ExecutionEnvironmentBuilder.create(executor, settings)
    val env = builder.build()
    ProgramRunnerUtil.executeConfiguration(env, false, true)
  end runMillTask

end MillModulesToolWindowFactory