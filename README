# Inquiry `metasvc`

This service is responsible for video fetching, processing and querying.

## Dependencies

The following dependencies are required for `metasvc` to run. These ensure that videos are processed properly, and all necessary metadata is collected.

* `ffmpeg = N-76045-g97be5d4`
* `tesseract-ocr = 3.03`
* `scala = 2.11.7`
* `sbt = 0.13.9`

Furthermore, `postgresql` must be installed to at least version `9.4.x` in order for `metasvc` to aggregate and query video metadata.

## Required Directories

The following directories are required for `metasvc` to run. These ensure that both logs and fetched data are stored and aggregated in the correct location.

* `/var/www/videos`
* `/var/www/frames`
* `/var/log/metasvc`

## Building

In order to build `metasvc`, simply run `sbt dist` on your terminal within the root folder of this repository. This should create a `metasvc-x.y.zip` inside of your `target/universal` folder. This is the production ZIP and can be deployed as is to a production server.

## Usage

`metasvc` attempts to use as much CPU / GPU and RAM as physically available in order to expedite the processing of video metadata upon a process request. This is because `metasvc` utilizes the Akka Actor System to spawn several threads and both concurrently / parallely process video frames. `ffmpeg` also utilizes as many threads as possible to split the video into frames.

In order to run the service, unzip the aforementioned `metasvc-x-y.zip` and navigate to the `bin` directory inside. Once there, execute the following command to start `metasvc`:

`sudo ./metasvc -Dconfig.resource=prod/application.conf -Dlogger.resource=prod/logback.xml`

This command ensures that the production configuration takes effect, and not the standard debug configuration.
