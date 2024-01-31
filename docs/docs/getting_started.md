# Developer Getting Started Guide CP4M

## Introduction:

_CP4M is a platform designed to accelerate the process of creating and deploying a LLM powered bot. It does so by being formed of multiple components that can be linked together and configured to fill in for the API Gateway, Data Storage, and/or the LLM API stages. Notably it will not directly support the Transformation & Enrichment, however it will support integrating with services that handle that stage._ 


## Table of Contents:

1. ### **Prerequisites**

   - System requirements

   - Java SDK and dependencies

2. ### **Installation**

   - Cloning the CP4M repository

   - Setting up dev environment and IDE

3. ### **Usage**

   - Building and Running Docker Containers

   - Building and Running locally

4. ### **Configuration**

   - Using configuration files

5. ### **Contributing**

   - Issues

   - Pull Requests

   - Contributor License Agreement

   - License

6. ### **FAQs**

   - Frequently asked questions and troubleshooting tips


## 1. Prerequisites

1. [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/?section=mac)

2. [Java Open JDK Version 21](https://jdk.java.net/21/)

3. [Registered FB Developer App (For testing and creating webhooks)](https://developers.facebook.com/docs/development/create-an-app/)

4. [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)  

5. [maven](https://maven.apache.org/install.html)

***


## 2. Installation

The following steps will help you get CP4M installed and running locally on your machine. 


### Clone the CP4M Repository 

- Clone the CP4M Repository from GitHub to your local machine using \`git clone\`. 

<!---->

    ``` git clone https://github.com/facebookincubator/CP4M.git ```


### Setting up dev environment and IDE

- Install IntelliJ IDEA Community Edition IDE - [Download Link](https://www.jetbrains.com/idea/download/?section=mac)

  - **Note: Make sure you install the Community Edition**


- Open IntelliJ and select Open Project or Get from VCS

![](https://lh7-us.googleusercontent.com/1JMNn0e7nm2c3ZPlBiBux9VPxV_t1LkSnHZ1sdHW1OLygxxQBj_g9fooDWLcfNLHl1aZTFZGIOrNsUI6-qQAO61RCTOZVRu9EDfyu_ja-J7xol0LYr4eyY9kKkjb5tLgW4vM8MV1jBxp-l4NstIgWvw)

- If you select open, you’ll have to browse to and select the CP4M directory you cloned in the previous step

![](https://lh7-us.googleusercontent.com/ngWrVBvcMTU6TdPXxmgb9OwPDFuRwcUq3MvbhP7-AeJ2xAsd67_1pLrCaTIdgiKAnu3KVfSTQYzD0nS5EqYyJxKc_qUG8EpXKU0U8FRo542Q35ltaZwgvpwvSmXsJSHhNwsI4lMCMY43cQRMgR3UDNU)

- If you select the “Get from VCS option” you’ll just need to input the URL from the git repo

![](https://lh7-us.googleusercontent.com/rTYSwdJd7Gog06fNZs4EvSrNMdS7BEGG2Z6fj7YUpaS2lqnyuoKS3zpbfpxjAn-W7YTUtmG4CAPTEECm-E-X4rW2kdDX8Jjp9wO01s-LQOFWiAcwb7MRCxAeYXTw1WeFUBpfeQcHhsK_KfFuD94S8jk)

- Once you have the project open you’ll want to make sure you have the correct version of the java SDK installed to ensure that all tests run successfully. 

![](https://lh7-us.googleusercontent.com/h8Xaatr_5W4_gAFBAnowRdDEsG9DBavrf1ld0W_YkD4oQa6VZsF5jNUyjRNqYop4yxD2NjwM-l212GpsaK7OYQ1Y2mqfA6nmbj_bxW3ivK4FkWOi5arrze8lFtcJd-cFdSpTig92OHHA3xJ-TUTe3mc)

- After you select the correct version of the SDK you’re now ready to run tests by clicking run test in the main test directory: 

  - **Note: MainTest does not currently run ALL tests, so you will have to run the different tests inside their respective directories**

![](https://lh7-us.googleusercontent.com/tuWo9pUHkCtYhjbFimjjXNVGB8P-gSLYbZ3dCh_O-eM7B6i85mZ965lzLCN2YkOLQyByL1zoHF7kwAG_vngyd1jrt7NKzVSQGsv2N3FsIEl95WK_AQpXR-D7naMqV3dj1U5J6LC0qyRm1Bdvt1ajTxc)

- You’ll also want to select the correct maven project by clicking on the maven tab on the right hand side of the IDE and selecting version 21 of the Java SDK. ( **note: You can also run CP4m from this tab by hitting the play button and selecting  `maven package`**)

![](https://lh7-us.googleusercontent.com/9MJLGXs61UNDhxq2d_C074IY75EvT83h8tPqgFOy_3HKGuCI2UGBICcujCO2LGodountNMx1lkpMLTyOlSTSncl8zrcWVWo7vI7Ja9194TBMUvHDEn-HwtIpt4g_EbTpOqtr19mfz1NjCTisLNaDoGc)


***

## 3. Usage

### Building and Running via Docker Containers

- These commands change your directory to the CP4M directory, build a docker image named cp4m and then run that image and expose port 8080 for webhooks.

```

    $ cd CP4M
    $ docker build -t cp4m .
    $ docker run -v /tmp/cp4m:/tmp/cpm4 -e CP4M_CONFIGURATION_FILE=/tmp/cp4m/cp4m.toml -p 8080:8080 cp4m 

```


### Building & Running CP4M Locally

- This command will build the project locally using maven and will run the resulting .jar file

```mvn clean -U package -Dcustom.jarName=cp4m -Dmanve.test.skip=true```


***

## 4. Configuration

### Using configuration files: 

Almost All of CP4M’s configurations are set via a configuration file. This file can be in either JSON or TOML format, we use TOML here for brevity. 

1. Create a file to hold the configuration

```
    mkdir /tmp/cp4m
    touch /tmp/cp4m/cp4m.toml
```

2. Copy on of the texts below into your newly created TOML file

**Example: Whatsapp & OpenAI**

    port = 8080

    [[plugins]]
    name = "openai_test"
    type = "openai"
    model = "gpt-3.5-turbo"
    api_key = "<your api key here>"

    [[stores]]
    name = "memory_test"
    type = "memory"
    storage_duration_hours = 1
    storage_capacity_mbs = 1

    [[handlers]]
    type = "whatsapp"
    name = "whatsapp_test"
    verify_token = "<your verification token here>"
    app_secret = "<your verification app secret here>"
    access_token = "<you access token here>"

    [[services]]
    webhook_path = "/whatsapp"
    plugin = "openai_test"
    store = "memory_test"
    handler = "whatsapp_test"

**Example: Messenger & LLama 2 (via Hugging Face)**

    port = 8080

    [[plugins]]
    name = "hf_test"
    type = "hugging_face"
    endpoint = "https://example.com"
    token_limit = 1000
    api_key = "<your api_token here>"

    [[stores]]
    name = "memory_test"
    type = "memory"
    storage_duration_hours = 1
    storage_capacity_mbs = 1

    [[handlers]]
    type = "messenger"
    name = "messenger_test"
    verify_token = "<your verification token here>"
    app_secret = "<your verification app secret here>"
    page_access_token = "<your page access token here>"

    [[services]]
    webhook_path = "/messenger"
    plugin = "openai_test"
    store = "memory_test"
    handler = "messenger_test"

      


***

## 5. Contributing

### Issues

- We use GitHub issues to track public bugs. Please ensure your description is clear and has sufficient instructions to be able to reproduce the issue.


### Pull Requests

We welcome pull requests.

1. Fork the repo and create your branch from master.

2. If you've added code that should be tested, add tests.

3. If you've changed or added APIs, update the documentation.

4. Make sure the test suite passes.

5. If you haven't already, complete the Contributor License Agreement ("CLA").


### Contributor License Agreement

In order to accept your pull request, we need you to submit a CLA. You only need to do this once to work on any of Facebook's open source projects.

Complete your CLA here: <https://code.facebook.com/cla>.


### License

By contributing to Spectrum, you agree that your contributions will be licensed under its MIT license.


***

## 6. FAQs



**What is a Handler in CP4M?**

A Handler handles incoming messages from Meta and contains logic to respond to those messages.

**What is a ChatStore in CP4M?**

A ChatStore stores messages in-memory and stores an entire threadState, which is a set of messages that comprise a thread of messages between a user and an assistant (bot).

**What is an LLMPlugin in CP4M?**

An LLMPlugin handles the process of sending a threadstate to a plugin such as OpenAI or LLama, and handles tokenization and request handling.

**What is a service runner in CP4M?**

A: A service runner is a component that holds a set of services and runs them, wrapping everything up and exposing it using the same Javelin.
