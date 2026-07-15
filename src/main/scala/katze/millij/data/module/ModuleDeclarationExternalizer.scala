package katze.millij.data.module

import com.intellij.util.io.{DataExternalizer, DataInputOutputUtil, IOUtil}
import katze.millij.data.{ScalaIdentifier, SegmentedPath}

import java.io.{DataInput, DataOutput}
import scala.jdk.CollectionConverters.*

class ModuleDeclarationExternalizer extends DataExternalizer[ModuleDeclaration[ScalaIdentifier]]:
  override def save(out: DataOutput, value: ModuleDeclaration[ScalaIdentifier]): Unit =
    DataInputOutputUtil.writeSeq(out, value.path.namespace.parts.asJava, tpe =>
      IOUtil.writeUTF(out, tpe.asString)
    )
    DataInputOutputUtil.writeSeq(out, value.path.path.parts.asJava, tpe =>
      IOUtil.writeUTF(out, tpe.asString)
    )
    DataInputOutputUtil.writeSeq(out, value.superTypes.asJava, tpe =>
      IOUtil.writeUTF(out, tpe.asQualified)
    )
  end save

  override def read(in: DataInput): ModuleDeclaration[ScalaIdentifier] =
    val filePath = SegmentedPath(
      DataInputOutputUtil.readSeq(in, () =>
        ScalaIdentifier.unsafe(IOUtil.readUTF(in))
      ).asScala.toList
    )
    val inFilePath = SegmentedPath(
      DataInputOutputUtil.readSeq(in, () =>
        ScalaIdentifier.unsafe(IOUtil.readUTF(in))
      ).asScala.toList
    )
    val entityTypes = DataInputOutputUtil.readSeq(in, () =>
      SegmentedPath.fromQualifiedNonEmpty(IOUtil.readUTF(in)).get.map(ScalaIdentifier.unsafe)
    ).asScala.toList

    ModuleDeclaration(NamespacedPath(filePath, inFilePath), entityTypes)
  end read
end ModuleDeclarationExternalizer