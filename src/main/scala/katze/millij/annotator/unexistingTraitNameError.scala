package katze.millij.annotator

import katze.millij.data.MillijBundle

def unexistingTraitNameError(name : String) : String =
  MillijBundle.message("unexisting.trait.name.error", name)
end unexistingTraitNameError