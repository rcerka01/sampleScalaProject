import sbt.{File, IO}
import sbtassembly.{MergeStrategy, PathList}

import scala.xml._
import scala.xml.dtd.{DocType, PublicID}

object ServiceAssembly {

  /**
    * The strategy to merge aop.xml files for all kamon components (kamon-scala, kamon-spray etc.) into one aop file.
    *
    * Discussion of the issue: https://github.com/kamon-io/Kamon/issues/59
    * Script source: https://gist.github.com/colestanfield/fac042d3108b0c06e952
    */
  val aopMerge: MergeStrategy = new MergeStrategy {
    val name = "aopMerge"

    def apply(tempDir: File, path: String, files: 0Seq[File]): Either[String, Seq[(File, String)]] = {
      val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
      val file = MergeStrategy.createMergeTarget(tempDir, path)
      val xmls: Seq[Elem] = files.map(XML.loadFile)
      val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
      val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
      val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
      val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
      val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
      val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
      val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
      XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
      IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
      Right(Seq(file -> path))
    }

  }

  val aspectjAopMergeStrategy: String => MergeStrategy = {
    case PathList("META-INF", "aop.xml") => aopMerge
    case s if s.endsWith("io.netty.versions.properties") => MergeStrategy.first
    case s => MergeStrategy.defaultMergeStrategy(s)
  }
}
