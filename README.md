# StackMob Scala Airbrake
A Scalaz enhanced version of msingleton's [Scala Airbrake client](https://github.com/msingleton/Scala-Airbrake) that uses Scalaz actors and IO.

## Usage
``` scala
val airbrake = new AirbrakeService {
  override def getApiKey = "your-key"
  override def getEnvironment = "development" // or staging, production, whatever...
}
airbrake.notifyAsync(AirbrakeNotice(exception)) // async (fire and forget)
// ... or ...
val result: Validation[Throwable, Int] = airbrake.notifySync(AirbrakeNotice(exception)) // sync
// ... or ...
val result: IO[Validation[Throwable, Int] = airbrake.notify(AirbrakeNotice(exception)) // IO
```
