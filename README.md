<!-- omit in toc -->
# Intune対応開発メモ

- [注意事項](#注意事項)
- [MSAL Android 導入](#msal-android-導入)
  - [参考URL](#参考url)
  - [依存関係を追加](#依存関係を追加)
  - [署名ハッシュを取得](#署名ハッシュを取得)
  - [MSAL設定ファイル](#msal設定ファイル)
  - [Androidマニフェストを編集](#androidマニフェストを編集)
- [Intune App SDK 導入](#intune-app-sdk-導入)
  - [参考URL](#参考url-1)
  - [SDKファイルをダウンロード](#sdkファイルをダウンロード)
  - [依存関係を追加](#依存関係を追加-1)
  - [AndroidManifest.xmlを編集](#androidmanifestxmlを編集)
- [認証処理を実装](#認証処理を実装)

## 注意事項

MSAL設定情報（AndroidManifest.xmlおよびres/raw/msal_config.jsonに記載）について、
サンプルではベタ書きしていますが、
ASTアプリではベタ書きせず、環境変数設定（？）で切り替えできるようにしてください。

私がAndroidアプリ開発に慣れていないため、
サンプルおよびこのメモに不可解な点があるかもしれません。
その場合は適切なやり方に修正してください。

## MSAL Android 導入

### 参考URL

- https://github.com/AzureAD/microsoft-authentication-library-for-android?tab=readme-ov-file

### 依存関係を追加

依存関係「com.microsoft.identity.client:msal」を追加します。（app/build.gradle.ktsを編集？）

リポジトリ「maven("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")」を追加します。（settings.gradle.ktsを編集？）


### 署名ハッシュを取得

APK署名に使うキーストアから、署名ハッシュを下記コマンドで取得します。

```sh
keytool -exportcert -alias SIGNATURE_ALIAS -keystore PATH_TO_KEYSTORE | openssl sha1 -binary | openssl base64
```

さらに署名ハッシュをURLエンコードした文字列を、下記コマンドで取得します。

```sh
# 例：署名ハッシュが「WWKsWNmxHAoZi6dIqRlv0EEY+8s=」の場合
node -p 'encodeURIComponent("WWKsWNmxHAoZi6dIqRlv0EEY+8s=")'
```

署名ハッシュはMSAL設定に利用します。

> キーストア（署名）は、常に同じものを使用し、異なる開発者間で共有するする必要があります。

### MSAL設定ファイル

configファイルを追加します。（app/src/main/res/raw/msal_config.json）

```json
{
  "client_id" : "（クライアントID）",
  "authorization_user_agent" : "DEFAULT",
  "redirect_uri" : "msauth://（パッケージ名）/（署名ハッシュをURIエンコードした文字列）",
  "authorities" : [
    {
      "type": "AAD",
      "audience": {
        "type": "AzureADMyOrg",
        "tenant_id": "（テナントID）"
      }
    }
  ],
  "account_mode": "SINGLE",
  "broker_redirect_uri_registered": true
}
```

サンプルではconfigファイルにMSAL設定情報をベタ書きしていますが、
ASTアプリでは環境変数設定など（？）を使って

- MSAL設定情報をGit管理外にする
- 検証環境・本番環境でMSAL設定情報を切り替えられる

ようにしてください。

### Androidマニフェストを編集

app/src/main/AndroidManifest.xmlを編集します。

下記を追加します。

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

さらに下記を追加します。

```xml
        <activity
            android:name="com.microsoft.identity.client.BrowserTabActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="msauth"
                    android:host="（パッケージ名）"
                    android:path="/（署名ハッシュ）" />
            </intent-filter>
        </activity>
```

サンプルではAndroidManifest.xmlに署名ハッシュをベタ書きしていますが、
ASTアプリでは環境変数設定など（？）を使って

- 署名ハッシュをGit管理外にする
- 検証環境・本番環境で署名ハッシュを切り替えられる

ようにしてください。

## Intune App SDK 導入

### 参考URL

- https://learn.microsoft.com/ja-jp/intune/intune-service/developer/app-sdk-android-phase3

### SDKファイルをダウンロード

https://github.com/microsoftconnect/ms-intune-app-sdk-android を git cloneします。

- Microsoft.Intune.MAM.SDK.aar
- GradlePlugin/com.microsoft.intune.mam.build.jar

上記ファイルをapp/libs（もしくはもっと適切なディレクトリ）にコピーします。

### 依存関係を追加

コピーしたMicrosoft.Intune.MAM.SDK.aarを依存関係に追加します。（app/build.gradle.ktsを編集？）

コピーしたcom.microsoft.intune.mam.build.jarおよびorg.javassist:javassist:3.29.2-GAをビルドツールに追加します。（build.gradle.ktsを編集？）

```
buildscript {
    dependencies {
        classpath("org.javassist:javassist:3.29.2-GA")
        classpath(files("app/libs/com.microsoft.intune.mam.build.jar"))
    }
}
```

プラグイン「id("com.microsoft.intune.mam")」を追加します。（app/build.gradle.ktsを編集？）

### AndroidManifest.xmlを編集

```xml
android:name="com.microsoft.intune.mam.client.app.MAMApplication"
```

## 認証処理を実装

認証処理を実装します。

MainActivity.kt（？）を編集して、下記コードを追加します。（詳細はサンプルコードを参照）

- initMsal()
  - onCreate()の中で呼び出します。
- sso()
- signIn()
- setAccount()
- initMam()
  - onCreate()の中で呼び出します。
- acquireTokenSilent()
