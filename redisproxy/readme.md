# Redis Cache
The Redis Cache is a simple implementation of an in-memory cache backed by a Redis instance 
service.  In addition, the Redis Cache provides a server that understand the Resp protocol 
and simply redirects the call the actual Redis instance.

# Architecture
![alt text](https://github.com/adam-p/markdown-here/raw/master/src/common/images/icon48.png "Logo Title Text 1")

# Design
## Principle
A fundamental design principle is to treat locks suspiciously and design without them.
Concurrency must be well thought out in the context of a single instance or a cluster of nodes.
Locks limit throughput.  Avoid them!  One solution is to use a responsive 
design versus a lock-and-check.
  
## Operation: Get Complexity
A cache is divided into buckets, otherwise known as segments in Ehcache. Determining 
the segment is based on a key hashing algorithm that is fundamentally driven off the 
object's hashcode.  Once in the bucket, a linear search is performed of each item in 
the segment.  Imperative to the complexity is the size of the segments (m) relative to the 
overall cache size (n).  Ideally, items must be equally distributed to a segment.  In the 
worst case, a single segment is created and every item in the cache is searched, resulting 
in an O(n) complexity.  That is the theoretical upper bound.  Assuming an equally 
distributed hashing function and an equally sized segments, a fraction of n/m items
will be searched, resulting in O(n/m) complexity.  Key for any of this to work is 
the actual hashing function.  Small bucket sizes with a direct mapping into them makes
for an operation closer to constant.
 
## Operation: LRU Eviction Complexity
An eviction takes place when the capacity of the cache is reached and elements in the 
cache need to be removed to make space.  A least-recently-used policy specifies that
an element least accessed is the ideal candidate for eviction.  Ehcache's default configuration 
is an LRU based strategy that takes a random sample of (m) elements and removes the least 
recently used of that subset.  In this case, the complexity is O(m).
  
## Operation: Global Expiration
Ehcache's global expiration strategy iterates through all items looking for elements that 
exceeded their time to live.  This is an O(n) operation.
  
# Concurrent Processing Analysis
In the event of a large number of simultaneous requests, the limiting factor on throughput 
will be:
* the size of the threadpool.  This will always dictate how many worker threads will 
  be available to process requests.  Requests will be queued up if all threads are working.
* if the threadpool size is large, you can expect the OS to thrash.  Putting 
  a silly large number of threads doesn't work.
* the distribution of hash codes received from each request.  The cache has read locks 
  (ick!).  If multiple requests map to the same segment, throughput is automatically 
  impacted. 
  
# Security
An API must always be behind some mechanism to authenticate and authorize users.  Similarly 
passwords must be stored in a secret store as opposed to a configuration file.  For the 
purposes of this assignment, the secret is in a configuration file, and there is no 
security on the API. (see [article](https://aws.amazon.com/blogs/mt/the-right-way-to-store-secrets-using-parameter-store/))

# Technology Stack
The specific technology stack was chosen because of its ease of use and ability to quickly 
wire a decent solution.  The following choices were made:

* Java
* Spring Boot (web,IoC)
* Ehcache (cache)
* Lettuce/Jedis ( redis client)
* Resp Server  (redis resp server/protocol)
    
# Test Strategy
The strategy is to create an end-to-end testing framework that exercises the 
production code.  Systems testcases were favored over integration and unit testcases 
solely because of time constraints. 

A redis client is used to populate the Redis instance directly and to exercise the Resp 
Proxy server.

# Building
In order to execute:
1.  Clone the repo `git clone redisproxy`
2.  Inside the redisproxy directory, execute the command `make test`
    * You will be prompted for configuration.  Feel free to use defaults.

# Deployment
This project does not deploy.  It is not part of the requirements.

# Disclaimers
This project was not tested on a modern Linux distribution nor on a Mac OS.  This 
development effort was done solely on a Windows based machine using cygwin.
The make file was not tested outside of Windows. 

# Time Spent
|Task/Service |    Time     | 
|:------------|:-----------:|
|WWW          |   30 min    |
|Redis Service|   10 min    |
|Ehcache      |    1 hr     |
|Resp Server  |    2 hr     |
|Docker       |   12 hr     |
|Makefile     |    2 hr     |
|Testcases    |    7 hr     |
|Documentation|    6 hr     |
|Configuration|    3 hr     |
