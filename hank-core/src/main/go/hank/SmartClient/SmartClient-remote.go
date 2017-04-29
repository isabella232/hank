/* Autogenerated by Thrift Compiler (0.9.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package main

import (
        "flag"
        "fmt"
        "http"
        "net"
        "os"
        "strconv"
        "thrift"
        "thriftlib/hank"
)

func Usage() {
  fmt.Fprint(os.Stderr, "Usage of ", os.Args[0], " [-h host:port] [-u url] [-f[ramed]] function [arg1 [arg2...]]:\n")
  flag.PrintDefaults()
  fmt.Fprint(os.Stderr, "Functions:\n")
  fmt.Fprint(os.Stderr, "  get(domain_name string, key string) (retval325 *HankResponse, err os.Error)\n")
  fmt.Fprint(os.Stderr, "  getBulk(domain_name string, keys thrift.TList) (retval326 *HankBulkResponse, err os.Error)\n")
  fmt.Fprint(os.Stderr, "\n")
  os.Exit(0)
}

func main() {
  flag.Usage = Usage
  var host string
  var port int
  var protocol string
  var urlString string
  var framed bool
  var useHttp bool
  var help bool
  var url http.URL
  var trans thrift.TTransport
  flag.Usage = Usage
  flag.StringVar(&host, "h", "localhost", "Specify host and port")
  flag.IntVar(&port, "p", 9090, "Specify port")
  flag.StringVar(&protocol, "P", "binary", "Specify the protocol (binary, compact, simplejson, json)")
  flag.StringVar(&urlString, "u", "", "Specify the url")
  flag.BoolVar(&framed, "framed", false, "Use framed transport")
  flag.BoolVar(&useHttp, "http", false, "Use http")
  flag.BoolVar(&help, "help", false, "See usage string")
  flag.Parse()
  if help || flag.NArg() == 0 {
    flag.Usage()
  }
  
  if len(urlString) > 0 {
    url, err := http.ParseURL(urlString)
    if err != nil {
      fmt.Fprint(os.Stderr, "Error parsing URL: ", err.String(), "\n")
      flag.Usage()
    }
    host = url.Host
    useHttp = len(url.Scheme) <= 0 || url.Scheme == "http"
  } else if useHttp {
    _, err := http.ParseURL(fmt.Sprint("http://", host, ":", port))
    if err != nil {
      fmt.Fprint(os.Stderr, "Error parsing URL: ", err.String(), "\n")
      flag.Usage()
    }
  }
  
  cmd := flag.Arg(0)
  var err os.Error
  if useHttp {
    trans, err = thrift.NewTHttpClient(url.Raw)
  } else {
    addr, err := net.ResolveTCPAddr("tcp", fmt.Sprint(host, ":", port))
    if err != nil {
      fmt.Fprint(os.Stderr, "Error resolving address", err.String())
      os.Exit(1)
    }
    trans, err = thrift.NewTNonblockingSocketAddr(addr)
    if framed {
      trans = thrift.NewTFramedTransport(trans)
    }
  }
  if err != nil {
    fmt.Fprint(os.Stderr, "Error creating transport", err.String())
    os.Exit(1)
  }
  defer trans.Close()
  var protocolFactory thrift.TProtocolFactory
  switch protocol {
  case "compact":
    protocolFactory = thrift.NewTCompactProtocolFactory()
    break
  case "simplejson":
    protocolFactory = thrift.NewTSimpleJSONProtocolFactory()
    break
  case "json":
    protocolFactory = thrift.NewTJSONProtocolFactory()
    break
  case "binary", "":
    protocolFactory = thrift.NewTBinaryProtocolFactoryDefault()
    break
  default:
    fmt.Fprint(os.Stderr, "Invalid protocol specified: ", protocol, "\n")
    Usage()
    os.Exit(1)
  }
  client := hank.NewSmartClientClientFactory(trans, protocolFactory)
  if err = trans.Open(); err != nil {
    fmt.Fprint(os.Stderr, "Error opening socket to ", host, ":", port, " ", err.String())
    os.Exit(1)
  }
  
  switch cmd {
  case "get":
    if flag.NArg() - 1 != 2 {
      fmt.Fprint(os.Stderr, "Get requires 2 args\n")
      flag.Usage()
    }
    argvalue0 := flag.Arg(1)
    value0 := argvalue0
    argvalue1 := flag.Arg(2)
    value1 := argvalue1
    fmt.Print(client.Get(value0, value1))
    fmt.Print("\n")
    break
  case "getBulk":
    if flag.NArg() - 1 != 2 {
      fmt.Fprint(os.Stderr, "GetBulk requires 2 args\n")
      flag.Usage()
    }
    argvalue0 := flag.Arg(1)
    value0 := argvalue0
    arg330 := flag.Arg(2)
    mbTrans331 := thrift.NewTMemoryBufferLen(len(arg330))
    defer mbTrans331.Close()
    _, err332 := mbTrans331.WriteString(arg330)
    if err332 != nil { 
      Usage()
      return
    }
    factory333 := thrift.NewTSimpleJSONProtocolFactory()
    jsProt334 := factory333.GetProtocol(mbTrans331)
    containerStruct1 := hank.NewGetBulkArgs()
    err335 := containerStruct1.ReadField2(jsProt334)
    if err335 != nil {
      Usage()
      return
    }
    argvalue1 := containerStruct1.Keys
    value1 := argvalue1
    fmt.Print(client.GetBulk(value0, value1))
    fmt.Print("\n")
    break
  case "":
    Usage()
    break
  default:
    fmt.Fprint(os.Stderr, "Invalid function ", cmd, "\n")
  }
}
