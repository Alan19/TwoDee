# TwoDee
Discord bot that generates probability odds to help players make better decisions. It also helps with automating game features, such as dice rolls and plot point management.

## Getting Started

Go to the Discord developer website and set up a new application and then generate a bot token. Have the token ready and don't give it to people you don't trust.

https://discordapp.com/developers/applications/

### Prerequisites

IntelliJ, Java 8, Gradle

### Installing

1. Open IntelliJ
2. Clone this repository with git clone
3. Open the bot.properties file and fill in fields with appropriate information
4. Add resource files
4. Run TwoDee to generate Google API token
5. To get a distribution, use ./gradle distzip
6. To run from the command line, use ./gradle run

## Built With

* [JavaCord](https://github.com/Javacord/Javacord) - An easy to use multithreaded library for creating Discord bots in Java.
* [Gradle](https://gradle.org/) - Dependency Management

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

Thanks to JavaCord for providing an easy to use library for creating Discord bots, [wadeanthony0100](https://github.com/wadeanthony0100) for helping me with web requests, and [SuicidalSteve](https://github.com/SuicidalSteve) for giving me the idea on how to generate statistics.
