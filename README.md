# StackMob Scala Airbrake
A Scalaz enhanced version of msingleton's [Scala Airbrake client](https://github.com/msingleton/Scala-Airbrake) that uses Scalaz actors and IO.

## Usage
``` scala
val airbrake = new AirbrakeService {
  override def getApiKey = "your-key"
  override def getEnvironment = "development" // or staging, production, whatever...
}
airbrake.notifyAsync(AirbrakeNotice(exception))
```
