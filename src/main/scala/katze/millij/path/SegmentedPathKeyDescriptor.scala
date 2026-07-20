package katze.millij.path

import cats.syntax.all.*
import com.intellij.util.io.KeyDescriptor

import java.io.{DataInput, DataOutput, IOException}

object SegmentedPathKeyDescriptor extends KeyDescriptor[SegmentedPath[List, String]]:
  override def getHashCode(t: SegmentedPath[List, String]): Int =
    t.hashCode()
  end getHashCode

  override def save(dataOutput: DataOutput, t: SegmentedPath[List, String]): Unit =
    dataOutput.writeUTF(t.asQualified)
  end save

  override def read(dataInput: DataInput): SegmentedPath[List, String] =
    SegmentedPath.fromQualified(dataInput.readUTF())
  end read

  override def isEqual(t: SegmentedPath[List, String], t1: SegmentedPath[List, String]): Boolean =
    t === t1
  end isEqual
end SegmentedPathKeyDescriptor
