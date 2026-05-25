package katze.millij.completions.insert

import com.intellij.codeInsight.completion.{InsertHandler, InsertionContext}
import com.intellij.codeInsight.lookup.LookupElement

object ExtendsArrayInsertHandler extends InsertHandler[LookupElement]:
  override def handleInsert(context: InsertionContext, item: LookupElement): Unit =
    val editor = context.getEditor
    val document = context.getDocument
    var tailOffset = context.getTailOffset
    val chars = document.getCharsSequence

    // 1. Check for existing colon
    var hasColon = false
    var searchOffset = tailOffset

    while searchOffset < chars.length() && chars.charAt(searchOffset) == ' ' do
      searchOffset += 1
    end while

    if searchOffset < chars.length() && chars.charAt(searchOffset) == ':' then
      hasColon = true
      tailOffset = searchOffset + 1
    end if

    if !hasColon then
      document.insertString(tailOffset, ": []")
      editor.getCaretModel.moveToOffset(tailOffset + 3)
    else
      document.insertString(tailOffset, " []")
      editor.getCaretModel.moveToOffset(tailOffset + 2)
    end if
  end handleInsert
end ExtendsArrayInsertHandler
