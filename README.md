
# 📊 Spring Boot - Native Images 
All builds and tests were done with GraalVM 21.

## Test Results
| bootBuildImage native-image                                 | + upx compression<br> + busybox runtime<br> + defaults                                                          | + upx compression<br> + busybox runtime<br> + mem optimizations<br> + default optimizations                                   | + upx compression<br> + busybox runtime<br> + mem optimizations<br>+ aggressive optimizations       | + upx compression<br> + busybox runtime<br> + mem optimizations<br>+ trade-off optimizations                                       | + upx compression<br> + busybox runtime<br> + mem optimizations<br> + default optimizations<br> + parallel gc                                          |
|-------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build Version                                               | 0.1.0                                                                                                           | 0.3.0                                                                                                                         | 0.4.0                                                                                               | 0.5.0                                                                                                                              | 0.6.0                                                                                                                                                  |
| Build Environment                                           | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-O2 default (good performance at a reasonable file size) | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-O2 default (good performance at a reasonable file size) | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-O3 (best performance) | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-O1 (trades performance for file size and build time) | BP_NATIVE_IMAGE<br>BP_BINARY_COMPRESSION_METHOD:upx<br>-R:MaxHeapSize=48m<br>-O2 default (good performance at a reasonable file size)<br> -gc=parallel | 
| Build Time                                                  | 11m 27s                                                                                                         | 8m 36s                                                                                                                        | 12m 29s                                                                                             | 11m 11s                                                                                                                            | 11m 9s                                                                                                                                                 |
| Build Image Size<br>Based on "busybox:stable-glibc"         | 39.2 MB                                                                                                         | 39.3 MB                                                                                                                       | 39.3 MB                                                                                             | 39.3 MB                                                                                                                            | 39.4 MB                                                                                                                                                |
| Container Memory Usage                                      | 274 MB (docker run w/o limit)<br>125 MB (docker run --memory=120m --memory-swap=0)                              | 197 MB (docker run w/o limit)<br> 90 MB (docker run --memory=116m --memory-swap=0)                                            | 203 MB (docker run w/o limit)<br> 88 MB (docker run --memory=116m --memory-swap=0)                  | 203 MB (docker run w/o limit)<br> 91 MB (docker run --memory=116m --memory-swap=0)                                                 | 199 MB (docker run w/o limit)<br> 90 MB (docker run --memory=116m --memory-swap=0)                                                                     | 
| CPU Usage Idle/Load                                         | 0% / 2%                                                                                                         | 0% / 2%                                                                                                                       | 0% / 20%                                                                                            | 0% / 12%                                                                                                                           | 0.2 % / mostly under 1%, seldom peaks to 18 %                                                                                                          | 
| Startup Time | \>= 1.3s                                                                                                        | \>= 1.3s                                                                                                                      | \>= 1.3s                                                                                            | \>= 1.3s                                                                                                                           | \>= 1.1s                                                                                                                                               |
| Endpoint Response Times<br>(GET localhost:8080)             | 4ms to 60ms  (mostly around 5 to 20ms)                                                                          | 6ms to 150ms  (mostly around 5 to 30ms)                                                                                       | 5ms to 170ms  (mostly around 5 to 30ms)                                                             | 4ms to 150ms  (mostly around 8 to 40ms)                                                                                            | mostly 1ms - 9 ms, sometimes > 10 ms, seldom peaks over 100ms                                                                                          | 
| Rating                                                      | ★★★☆☆<br>(missing memory optimizations)                                                                        | ★★★★☆                                                                                                                         | ★★★☆☆<br>(optimizations not as good as expected, long build time)                                  | ★★★☆☆                                                                                                                                    | ★★★★★<br>(best response times)                                                                                                                        |


The other GraalVM [optimization levels](https://www.graalvm.org/jdk25/reference-manual/native-image/optimizations-and-performance/) -Ob and -Os were not considered:
- The level -Ob produced a binary which was worse than all other variants.
- The level -Os is not available in GraalVM 21, whereas it is documented in the reference manual for GraalVM 25.<br>

Common build image size optimizations
* Paketo's Java Native Image Buildpack uses mostly-static images as default, so a distro-less base image having glibc is sufficient as runtime OS. Each build has been done with "busybox:stable-glibc" as runtime image to optimize the build image size.
* Furthermore each native image has been compressed during build by the [Ultimate Packer for eXecutables](https://upx.github.io/).

### Build and Test Environment
`Kernel: Linux 6.8.0-83-generic`<br>
`CPU: Intel(R) Core(TM) i3-4160T (4) @ 3.10 GHz`<br>
`GPU: Intel 4th Generation Core Processor Family Integrated Graphics Controller @ 1.15 GHz [Integrated]`<br>
`Memory: 12.49 GiB / 15.49 GiB (81%)`<br>
`Swap: 4.55 GiB / 16.76 GiB (27%)`<br>

### Building
Build the native image:
```
$ ./gradlew clean bootBuildImage
```

### Running
Run the image with docker:
```
$ docker run --rm --memory=116m --memory-swap=0 --name "spring-boot-native-image" -p 8080:8080 spring-boot-native-image:latest
```

## References
* [Paketo Buildpacks Java How To](https://paketo.io/docs/howto/java/)
* [Paketo Buildpack for Native Image](https://github.com/paketo-buildpacks/native-image)
* [Pack CLI](https://buildpacks.io/docs/for-platform-operators/how-to/integrate-ci/pack/)
* [GraalVM Manual](https://www.graalvm.org/latest/reference-manual/)
* [GraalVM - Native Image Build Output](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/BuildOutput.md)
