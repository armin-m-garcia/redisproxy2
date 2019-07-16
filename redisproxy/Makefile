os := $(shell uname -o)
docker_cmd := docker
cwd := $(shell pwd)

ifeq ($(os),Cygwin)
docker_cmd := winpty $(docker_cmd)
cwd := $(shell cygpath -m $(cwd))
endif

test : configure
	$(docker_cmd) run -it -v $(cwd):/xuxa opennms/maven /bin/bash -c "cd /xuxa; mvn test"

configure : docker-image.ts FORCE
	$(docker_cmd) run -it -v $(cwd):/xuxa opennms/maven /bin/bash -c "cd /xuxa; ./src/main/bash/configure"

docker-image.ts :
	$(docker_cmd) pull opennms/maven
	touch docker-image.ts

clean : 
	rm docker-image.ts
	$(docker_cmd) run -it -v $(cwd):/xuxa opennms/maven /bin/bash -c "cd /xuxa; mvn clean"
	$(docker_cmd) rm -f $(shell docker ps -aq)
	$(docker_cmd) rmi -f $(shell docker images opennms/maven:latest -q)

FORCE:
 
  
