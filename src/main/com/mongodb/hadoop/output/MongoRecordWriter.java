// MongoRecordWriter.java
/*
 * Copyright 2010 10gen Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.hadoop.output;

import java.io.*;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.bson.*;

import com.mongodb.*;
import com.mongodb.hadoop.*;

public class MongoRecordWriter<K,V> extends RecordWriter<K, V> {

    public MongoRecordWriter(DBCollection c , TaskAttemptContext ctx) {
        _collection = c;
        _context = ctx;
    }

    public void close( TaskAttemptContext context ){
        _collection.getDB().getLastError();
    }

    Object toBSON( Object x ){
        if ( x == null )
            return null;
        if ( x instanceof Text )
            return x.toString();
        if ( x instanceof IntWritable )
            return ( (IntWritable) x ).get();
        throw new RuntimeException( "can't convert: " + x.getClass().getName() + " to BSON" );
    }

    public void write( K key , V value ) throws IOException{
        final DBObject o = new BasicDBObject();

        if ( key instanceof MongoOutput ) {
            ( (MongoOutput) key ).appendAsKey( o );
        }
        else if ( key instanceof BSONObject ) {
            o.put( "_id", key );
        }
        else {
            o.put( "_id", toBSON( key ) );
        }

        if ( value instanceof MongoOutput ) {
            ( (MongoOutput) value ).appendAsValue( o );
        }
        else if ( value instanceof BSONObject ) {
            o.putAll( (BSONObject) value );
        }
        else {
            o.put( "value", toBSON( value ) );
        }

        try {
            _collection.save( o );
        }
        catch ( final MongoException e ) {
            throw new IOException( "can't write to mongo" , e );
        }
    }

    public TaskAttemptContext getContext(){
        return _context;
    }

    final DBCollection _collection;
    final TaskAttemptContext _context;
}
