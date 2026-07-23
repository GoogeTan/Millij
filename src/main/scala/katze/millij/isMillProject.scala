package katze.millij

import com.intellij.openapi.project.{Project, ProjectUtil}

def isMillProject(project : Project) : Boolean = 
  Option(ProjectUtil.guessProjectDir(project)).fold(false)(dir =>
    dir.findChild("build.mill") != null 
      || dir.findChild("build.mill.yaml") != null
      || dir.findChild("build.sc") != null
  )
end isMillProject
