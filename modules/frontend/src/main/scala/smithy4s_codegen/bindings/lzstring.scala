package smithy4s_codegen.bindings

import scala.scalajs.js
import js.annotation.JSImport

@JSImport("lz-string", JSImport.Namespace)
@js.native
object lzstring extends js.Object {
  def compressToEncodedURIComponent(input: String): String = js.native
  def decompressFromEncodedURIComponent(compressed: String): String = js.native
}
