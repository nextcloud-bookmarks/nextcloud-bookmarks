## :link: Nextcloud Bookmarks Android App

[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://gitlab.com/bisada/OCBookmarks/activity)
[![Android CI](https://gitlab.com/bisada/OCBookmarks/badges/master/pipeline.svg)](https://gitlab.com/bisada/OCBookmarks/-/pipelines)
[![Gitter](https://badges.gitter.im/nextcloud-bookmarks/community.svg)](https://gitter.im/nextcloud-bookmarks/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Open a new issue](https://img.shields.io/badge/Open%20Feature-Request-1abc9c.svg)](https://gitlab.com/bisada/OCBookmarks/-/issues)
[![Connect it on telegram](https://img.shields.io/badge/Connect%20via-%20telegram-0088cc.svg)](https://t.me/nextcloudbookmarks)

## :arrow_forward: Access
[![F-Droid Release](https://img.shields.io/f-droid/v/org.schabi.nxbookmarks)](https://f-droid.org/en/packages/org.schabi.nxbookmarks/) 

[<img src="https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png"
      alt="Donate with PayPal"
      height="80">](https://www.paypal.me/biswajitbangalore)
[<img src="https://raw.githubusercontent.com/stefan-niedermann/DonateButtons/master/LiberaPay.png"
      alt="Donate using Liberapay"
      height="80">](https://liberapay.com/bisasda/donate)

[<img src="assets/nx/icon.png" width=160px>](/)
[![F-Droid](./assets/fdroid_badge.png)](https://f-droid.org/packages/org.schabi.nxbookmarks/)
[![Amazon](https://images-na.ssl-images-amazon.com/images/G/01/mobile-apps/devportal2/res/images/amazon-appstore-badge-english-white.png)](https://www.amazon.com/dp/B08L5RKHMM/ref=apps_sf_sta)


## :eyes: Screenshots

| Multiple Accounts | SSO | Tags |  Bookmarks |
| :--: | :--: | :--: | :--: |
| ![Screenshot of list view](fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg) | ![Screenshot of edit mode](fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg)  | ![Screenshot of tag](fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg) | ![Screenshot of bookmark](fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg) |



## :rocket: Features


* Works offline 🔌
* Mark bookmarks as favorite Organize your bookmarks with labels 🔖
* Manage tags 🏷
* Translated in many languages 🌎
* Multiple accounts
* SSO : Nextcloud Single Sign On


An Android front end for the Nextcloud [Bookmark App](https://github.com/nextcloud/bookmarks/) 
based on the new [REST API](https://github.com/nextcloud/bookmarks/#rest-api) that was introduced
by NextCloudBookmarks version [3.2.1](https://github.com/nextcloud/bookmarks/releases/tag/v3.2.1)

[<img src="assets/nx/screenshots/1.jpg" width=160px>](assets/nx/screenshots/1.jpg)
[<img src="assets/nx/screenshots/2.jpg" width=160px>](assets/nx/screenshots/2.jpg)
[<img src="assets/nx/screenshots/3.jpg" width=160px>](assets/nx/screenshots/3.jpg)
[<img src="assets/nx/screenshots/4.jpg" width=160px>](assets/nx/screenshots/4.jpg)
[<img src="assets/nx/screenshots/5.jpg" width=160px>](assets/nx/screenshots/5.jpg)


## :checkered_flag: Planned features

* [Folder Structure](https://gitlab.com/bisada/OCBookmarks/issues/17)

## :family: Join the team

  * Test the app with different devices
  * Report issues in the [issue tracker](https://gitlab.com/bisada/OCBookmarks/issues)
  * [Pick an issue](https://gitlab.com/bisada/OCBookmarks/-/issues?label_name%5B%5D=help+wanted) :notebook:
  * Create a [Pull Request](https://opensource.guide/how-to-contribute/#opening-a-pull-request)
  * Buy this app on [Amazon App Store](https://www.amazon.com/dp/B08L5RKHMM/ref=apps_sf_sta)
  * Send me a bottle of your favorite beer :beers: :wink:
  * [![Connect it on telegram](https://img.shields.io/badge/Connect%20via-%20telegram-0088cc.svg)](https://t.me/nextcloudbookmarks)

## :link: Issues
* Please note we have identified Some issues. Please look at [Issue board](https://gitlab.com/bisada/OCBookmarks/issues) before review.
* Feel free to send us a pull request.
## :link: Maintainer
* [Biswajit Das](https://gitlab.com/bisasda):@bisasda

## :link: How to compile the App

## :label: Requirements:
-------------
  1. Android Studio

:arrow_down_small: Download and install:

  1. Open cmd/terminal
  2. Navigate to your workspace
  3. Then type in: `git clone https://gitlab.com/bisada/OCBookmarks.git`
  4. Import the Project in Android Studio and start coding!

## :link: Contributors
* [Biswajit Das](https://gitlab.com/bisasda):@bisasda
* [Christian Schabesberger](https://gitlab.com/derSchabi):@derSchabi

## :link: Requirements
* [Nextcloud](https://nextcloud.com/) instance running.
* [Nextcloud Android](https://github.com/nextcloud/android) app installed (> 3.9.0)
* [Nextcloud Bookmark](https://github.com/nextcloud/bookmarks) app enabled on Instances


# :link: Testing: Nextcloud Bookmarks Android App testing Guide

### Prerequisites

* You should have nextcloud instances access
* NextCloud Bookmarks version [3.2.1](https://github.com/nextcloud/bookmarks/releases/tag/v3.2.1) should be installed.

### Login to Bookmark App

 * Open the Android App "Nextcloud Bookmarks"
 * **Step 1:** Click on **Nextcloud Singn on (SSO)**.
 * **Step 2:** Register the Nextcoud app for sso: 
    * Enter the "server address" in the field. Eg: https://us.cloudamo.com/
    * Enter the Username in the **user name** field. eg. email id(biswajitxxxxxxxx@nextcloud.com)
    * Enter the credentials **Password** field.
    * Finally click on **SIGN IN** button.
    * Once added select the account to continue.
 * **Step 3:** It will open the **BOOKMARKS** screen
 * **Step 4:** Click on the **TAGS** tab to open TAGS screen.


| Step 1 SSO Login | Step 2 Select account | Step 3 Bookmarks screen |  Step 4 Tags Screen |
| :--: | :--: | :--: | :--: |
| ![Screenshot of list view](assets/nx/screenshots/3.jpg) | ![Screenshot of edit mode](assets/nx/screenshots/2.jpg)  | ![Screenshot of tag](assets/nx/screenshots/4.jpg) | ![Screenshot of bookmark](assets/nx/screenshots/5.jpg) |

### ADD New BookMark

* **Add Bookmarks:** To add New **bookmark** / **Tag** Click on the **+** (plus sign)
    * Add the intended url in **URL** field. Eg: https://www.youtube.com/user/Computerap
    * Add some meaningfull Title or Description.
    * Click on **+** (plus button) to add Tags to it. Eg. **youtube** . You can add multiple tags if you want.
    * Hit the **SAVE** button to add the **Bookmarks**.

| Add Bookmarks screen |
| :--: |
| ![Screenshot of list view](assets/nx/screenshots/6.jpg) |

### EDIT/Delete Bookmark/Tags

*  **Edit/Delete:** To edit or delete Bookmarks please long press on the **Bookmarks** or **Tags** this will open EDIT/DELETE/SHARE window.

| Edit/Delete screen |
| :--: |
| ![Screenshot of list view](assets/nx/screenshots/7.jpg) |

## :link: Contributions
* All pull requests are welcome.

[![ForTheBadge built-with-love](http://ForTheBadge.com/images/badges/built-with-love.svg)](https://gitlab.com/bisada/)
