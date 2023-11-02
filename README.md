# Conversation Platform 4 Marketing (CP4M)

CP4M is a conversational marketing platform which enables advertisers to integrate their customer-facing chatbots with
FB Messenger/WhatsApp, in order to meet customers where they are and drive native conversations on the advertiser's
owned infra.

# Details

CP4M acts as a bridge between existing Messenger and Whatsapp APIs and existing LLM APIs (OpenAI, LLaMa, etc.)  
![cp4m_diagram](https://github.com/facebookincubator/CP4M/assets/6844618/601433ff-c77d-4d52-a6f8-4f3b0ff45aae)

## Quickstart

### 1 Configuration file

Almost all of CP4M's configurations are set via a configuration file. This file can be in either JSON or TOML format, we
use TOML here for brevity.

#### 1.1 create a file to hold the configuration

```bash
$ mkdir /tmp/cp4m
$ touch /tmp/cp4m/cp4m.toml
```

#### 1.2 copy one of the texts below into your newly created TOML file

##### Example: Whatsapp & OpenAI

```toml
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

##### Example: Messenger & Llama 2 (via Hugging Face)

```toml
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

### 2 Build and Run CP4M

We provide a [Dockerfile](./Dockerfile) to build and run CP4M however you're also welcome to follow the steps in the
[Dockerfile](./Dockerfile) to build locally.

#### 2.1 Clone repo

```bash
$ git clone git@github.com:facebookincubator/CP4M.git
```

#### 2.2 build and run CP4M via docker

The below commands changes your directory to the CP4M directory, builds a docker image named `cp4m` and then runs that
images and exposing the port 8080 for webhooks.

```bash
$ cd CP4M
$ docker build -t cp4m .
$ docker run -v /tmp/cp4m:/tmp/cp4m -e CP4M_CONFIGURATION_FILE=/tmp/cp4m/cp4m.toml -p 8080:8080 cp4m
```

## License

CP4M is [MIT licensed](./LICENSE).
