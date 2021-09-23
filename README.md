[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]


<img src="images/icon.png" alt="Logo" align="right" width="80" height="80">
<h1 align="center">Welcome to TwoDee 👋</h1>


> A Discord bot for the Facets tabletop RPG ruleset.

### 🏠 [Homepage](https://github.com/Alan19/TwoDee)

## Install

This is an example of how you may give instructions on setting up your project locally. To get a local copy up and
running follow these simple example steps.

### Prerequisites

* Gradle

### Installation

1. Get a bot token from [Discord](https://discordapp.com/developers/applications/). **Do not share this with anyone!**
2. Clone the repo
   ```sh
    https://github.com/Alan19/TwoDee.git
    ```
3. Add announcement channels
4. Setup bot properties
5. Add Storyteller role ID
6. Setup `resources` folder
   ```JS
   const API_KEY = 'ENTER YOUR API';
   ```

### Adding Google Sheets integration

1. Download Google Drive API token
2. Fill out `players.json`

## Usage

You can run the bot with the following command

```sh
gradle run
```

*For examples of available commands, please refer to
the [documentation](https://github.com/Alan19/TwoDee/wiki/Bot-Commands)*

### Inviting the bot to your server

### Google Sheets

If this is the first time you have started the bot after adding Google Sheets integration, your browser automatically
open and send you to a page to authenticate your Google account. Once you do so, it will create a `tokens` folder in the
root directory of your project and download a `StoredCredential` file.

**Do not share the `credentials.json` file or the `StoredCredential` files!**

## Author

👤 **Alan19**

* Github: [@Alan19](https://github.com/Alan19)
* LinkedIn: [@alan-xiao1](https://www.linkedin.com/in/alan-xiao1/)

## 🤝 Contributing

Contributions, issues and feature requests are welcome!<br />Feel free to
check [issues page](https://github.com/Alan19/TwoDee/issues).

## Show your support

Give a ⭐️ if this project helped you!

## 📝 License

Copyright © 2021 [Alan19](https://github.com/Alan19). <br/>
This project is [MIT](https://github.com/Alan19/TwoDee/blob/master/LICENSE) licensed.

***
_This README was generated with ❤️ by [readme-md-generator](https://github.com/kefranabg/readme-md-generator)_


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->

[contributors-shield]: https://img.shields.io/github/contributors/Alan19/TwoDee.svg?style=for-the-badge

[contributors-url]: https://github.com/Alan19/TwoDee/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/Alan19/TwoDee.svg?style=for-the-badge

[forks-url]: https://github.com/Alan19/TwoDee/network/members

[stars-shield]: https://img.shields.io/github/stars/Alan19/TwoDee.svg?style=for-the-badge

[stars-url]: https://github.com/Alan19/TwoDee/stargazers

[issues-shield]: https://img.shields.io/github/issues/Alan19/TwoDee.svg?style=for-the-badge

[issues-url]: https://github.com/Alan19/TwoDee/issues

[license-shield]: https://img.shields.io/github/license/Alan19/TwoDee.svg?style=for-the-badge

[license-url]: https://github.com/Alan19/TwoDee/blob/master/LICENSE.txt

[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555

[linkedin-url]: https://www.linkedin.com/in/alan-xiao1/

[product-screenshot]: images/icon.png