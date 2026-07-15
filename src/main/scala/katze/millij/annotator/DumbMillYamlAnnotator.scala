package katze.millij.annotator

import katze.millij.cool.CoolPattern
import katze.millij.place.*

final class DumbMillYamlAnnotator extends DumbAnnotators(
  List(
    DumbCoolAnnotatorAdapter(
      objectInInappropriatePlace,
      CoolPattern.elementAndParent()
    ),
  )
)
