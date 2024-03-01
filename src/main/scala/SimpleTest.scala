import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object SimpleTest extends App {
  def execute(): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    lazy val sink = new DummySink()

    env
      .addSource(new IntSource)
      .uid("IntSourceOp").name("randomIntegers")
      .addSink(sink.invoke _).name("DummySink")

    env.execute("SimpleTest")
  }

  execute()
}

class IntSource extends SourceFunction[Int] {
  private var running = true

  override def run(ctx: SourceFunction.SourceContext[Int]): Unit = {
    val rand = new scala.util.Random
    while (running) {
      ctx.collect(rand.nextInt(Int.MaxValue))
      Thread.sleep(1000)
    }
  }

  override def cancel(): Unit = {
    running = false
  }
}

class DummySink extends SinkFunction[Int] {

  override def invoke(item: Int): Unit =
  {
    println(s"We sank the item $item")
  }
}
