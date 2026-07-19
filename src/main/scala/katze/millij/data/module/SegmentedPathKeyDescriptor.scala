package katze.millij.data.module

import cats.syntax.all.*
import com.intellij.util.io.KeyDescriptor
import katze.millij.data.{ScalaIdentifier, SegmentedPath}

import java.io.{DataInput, DataOutput}

object SegmentedPathKeyDescriptor extends KeyDescriptor[SegmentedPath[List, ScalaIdentifier]]:
  override def getHashCode(t: SegmentedPath[List, ScalaIdentifier]): Int =
    t.hashCode()
  end getHashCode

  override def save(dataOutput: DataOutput, t: SegmentedPath[List, ScalaIdentifier]): Unit =
    dataOutput.writeUTF(t.asQualified)
  end save

  override def read(dataInput: DataInput): SegmentedPath[List, ScalaIdentifier] =
    SegmentedPath.fromQualified(dataInput.readUTF()).traverse(ScalaIdentifier.fromStringOption).get//TODO
  end read

  override def isEqual(t: SegmentedPath[List, ScalaIdentifier], t1: SegmentedPath[List, ScalaIdentifier]): Boolean =
    t === t1
  end isEqual
end SegmentedPathKeyDescriptor
