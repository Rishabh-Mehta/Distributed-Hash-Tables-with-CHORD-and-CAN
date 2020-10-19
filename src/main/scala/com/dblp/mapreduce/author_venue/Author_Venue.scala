package com.dblp.mapreduce.author_venue

import java.io.ByteArrayInputStream
import java.lang

import com.dblp.mapreduce.utils.{ParseUtils, Utils}
import javax.xml.stream.XMLInputFactory
import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapreduce.{Mapper, Reducer}
import org.apache.log4j.BasicConfigurator
import org.slf4j.LoggerFactory

import scala.collection.View.Empty.take
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala
//import scala.xml.{Elem, XML}

object Author_Venue {
  BasicConfigurator.configure()
  val logger = LoggerFactory.getLogger(Author_Venue.getClass)




  class Map extends Mapper[LongWritable, Text, Text, IntWritable] {


    override def map(key: LongWritable, value: Text,
                     context: Mapper[LongWritable, Text, Text, IntWritable]#Context): Unit = {

      try {
        //val document = ParseUtils.formatXml(value.toString).toString()
        var document = value.toString
        document = document.replaceAll("\n", "").replaceAll("&", "&amp;").replaceAll("'", "&apos;").replaceAll("^(.+)(<)([^>/a-zA-z_]{1}[^>]*)(>)(.+)$", "$1&lt;$3&gt;$5")
        val reader = XMLInputFactory.newInstance.createXMLStreamReader(new ByteArrayInputStream(document.getBytes()))
        var authorCount = 0
        val outValue = new IntWritable(1)
        val venueMap = Utils.getVenueMap()
        var firsttag = true
        var xmlElement = ""
        var venue = ""
        var author =""

        while (reader.hasNext) {
          try {
            reader.next
            if (reader.isStartElement) {

              if (firsttag) {

                xmlElement = reader.getLocalName

                if (venueMap.exists(vMap => vMap._1 == xmlElement)) {
                  venue = venueMap.get(xmlElement).get

                }
                firsttag = false
              }
              val currentElement = reader.getLocalName
              if (currentElement eq "author") {
                authorCount += 1
                author = reader.getElementText
                context.write(new Text(author+","+venue),outValue)
                logger.debug("Mapper Output "+author+" , "+venue+" :",outValue)
              }
            }
          }
          catch {
            case e: Exception =>
              logger.error("Error parsing XML", e)
          }
        }
        reader.close()
//        if (authorCount > 0 && venue != "") {
//          val output = authorCount + "-" + venue
//          print("OUTPUT"+output+"\n")
//          logger.info("OUTPUT"+output)
//          context.write(new Text(output), outValue)
//        }
      }
      catch {
        case e: Exception =>
          logger.error("Error in parsing XML", e)
          throw new Exception(e)
      }
    }
  }

  class Reduce extends Reducer[Text, IntWritable, Text, IntWritable] {
    var result = new IntWritable
    override def reduce(key: Text, values: lang.Iterable[IntWritable], context: Reducer[Text, IntWritable, Text, IntWritable]#Context): Unit = {
      var sum = 0
      val scalaValues = values.asScala
      //val keys = key.toString.split(",")
      scalaValues.foreach(values => sum += values.get)
      result.set(sum)
      logger.info(key.toString)
      logger.debug("\n"+"+Reducer Output "+key+":"+result+"\n")
      context.write(key, result)

    }


  }


}
