# Richsploit 
Richsploit: Exploitation toolkit for RichFaces.

<b>Overview</b>

Richsploit can be used to exploit JSF endpoints using RichFaces. All versions from 3.1.0 and higher are vulnerable.
<pre>
usage: Richsploit
 -e,--exploit <arg>   0: CVE-2013-2165
                 1: CVE-2015-0279
                 2: CVE-2018-12532
                 3: CVE-2018-12533
                 4: CVE-2018-14667
 -p,--payload <arg>   The file containing serialized object
                 (CVE-2013-2165), or
                 Shell command to execute (all other CVE's)
 -u,--url <arg>       URL of richfaces application, i.e.
                 http://example.com/app for RF4.x and
                 http://example.com/app/a4j/g/3_3_3.Final for RF3.x
 -v,--version <arg>   Richfaces branch, either 3 or 4

</pre>

For more information about how to use the tool, please see [this blog post](https://www.redtimmy.com/java-hacking/richsploit-one-tool-to-exploit-all-versions-of-richfaces-ever-released/).
