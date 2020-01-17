## Setup

Get token with following scope
`https://mail.google.com/`

## Deploy AWS

Bot does not store access token in db and refreshes it every invocation.
So, to avoid race condition, use reserved concurrency `1`. 

## Configuration

Handler: `com.github.mrramych.shakalbot.Handler::handle`

* `google_client_id`
* `google_client_secret`
* `google_token_access` - can be any value
* `google_token_refresh`

* `sqs_url`

* `vk_group` - your group id
* `vk_target` - where to send messages
* `vk_token`
