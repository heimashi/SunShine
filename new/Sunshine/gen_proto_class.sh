#!/bin/bash
rm -f src/com/sw/sun/proto/* 
protoc --java_out=src proto/chat.proto
protoc --java_out=src proto/relation.proto
protoc --java_out=src proto/userprofile.proto