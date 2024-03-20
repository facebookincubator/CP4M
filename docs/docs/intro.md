---
sidebar_position: 1
---

# Introduction

Conversational Platform for Marketing (CP4M) is a middleware service which enables developers to easily integrate their user-facing LLM chatbots with messaging platforms like Whatsapp and Facebook Messenger. CP4M can help scale your chatbot's traffic to billions of people globally **in less than an hour** of development time.

![System Diagram](./img/cp4m_diagram.png)

## Getting Started

 Clone the open source project from [GitHub](https://github.com/facebookincubator/CP4M).

```bash
git clone git@github.com:facebookincubator/CP4M.git
```


### Set up your configuration file

1. Create an empty file
```bash
mkdir /tmp/cp4m
touch /tmp/cp4m/cp4m.toml
```

2. Copy the relevant configuration contents into your file

**Example: Whatsapp & OpenAI**
```bash
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
```

**Example: Messenger & Llama 2 (via Hugging Face)**
```bash
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
```


### Build and Run CP4M via Docker

```bash
docker build -t cp4m .
docker run -v /tmp/cp4m:/tmp/cp4m -e CP4M_CONFIGURATION_FILE=/tmp/cp4m/cp4m.toml -p 8080:8080 cp4m
```
