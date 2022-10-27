
useLogger(CustomEventLogger())

  class CustomEventLogger() : BuildAdapter(), TaskExecutionListener {
    override fun beforeExecute(task: Task) { }
    override fun afterExecute(task: Task, state: TaskState) { }
    override fun buildFinished(result: BuildResult){
      if (result.failure != null)
      {
        println("Build complete with errors.")
      }
      else
      {
        println("Build complete.")
      }
    }

  }
