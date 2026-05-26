package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.psi.PsiElement
import katze.millij.cool.{CoolPattern, PsiElementMatcher, PsiParentElementMatcher}

/**
 * Type safe annotator based on [[CoolPattern]]
 */
type CoolAnnotator[Data] = (Data, AnnotationHolder) => Unit