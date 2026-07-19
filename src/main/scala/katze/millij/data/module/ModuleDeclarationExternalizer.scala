package katze.millij.data.module

import com.intellij.util.io.{DataExternalizer, DataInputOutputUtil, IOUtil}
import katze.millij.data.SegmentedPath

import java.io.{DataInput, DataOutput, IOException}
import scala.jdk.CollectionConverters.*

class ModuleDeclarationExternalizer extends DataExternalizer[ModuleDeclaration[String]]:
  override def save(out: DataOutput, value: ModuleDeclaration[String]): Unit =
    DataInputOutputUtil.writeSeq(out, value.path.namespace.parts.asJava, tpe =>
      IOUtil.writeUTF(out, tpe)
    )
    DataInputOutputUtil.writeSeq(out, value.path.path.parts.asJava, tpe =>
      IOUtil.writeUTF(out, tpe)
    )
    DataInputOutputUtil.writeSeq(out, value.superTypes.asJava, tpe =>
      IOUtil.writeUTF(out, tpe.asQualified)
    )
  end save

  override def read(in: DataInput): ModuleDeclaration[String] =
    val filePath = SegmentedPath(
      DataInputOutputUtil.readSeq(in, () =>
        IOUtil.readUTF(in)
      ).asScala.toList
    )
    val inFilePath = SegmentedPath(
      DataInputOutputUtil.readSeq(in, () =>
        IOUtil.readUTF(in)
      ).asScala.toList
    )
    val entityTypes = DataInputOutputUtil.readSeq(in, () =>
      SegmentedPath.fromQualifiedNonEmpty(IOUtil.readUTF(in)).getOrElse(
        throw IOException(s"Incorrect path")
      )
    ).asScala.toList

    ModuleDeclaration(NamespacedPath(filePath, inFilePath), entityTypes)
  end read
end ModuleDeclarationExternalizer