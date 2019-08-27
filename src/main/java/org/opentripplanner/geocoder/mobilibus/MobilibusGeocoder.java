package org.opentripplanner.geocoder.mobilibus;

import org.bson.Document;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResults;
import org.opentripplanner.geocoder.google.GoogleGeocoder;
import org.opentripplanner.geocoder.nominatim.NominatimGeocoder;

import java.util.Arrays;
import java.util.Date;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.vividsolutions.jts.geom.Envelope;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobilibusGeocoder implements Geocoder {
	private static final Logger LOG = LoggerFactory.getLogger(MobilibusGeocoder.class);

	private NominatimGeocoder nominatimGeocoder;
	private GoogleGeocoder googleGeocoder;
	private MongoDatabase mobiMongoDb;

	private JSONDeserializer deserializer;
	private JSONSerializer serializer;
	
	public MobilibusGeocoder() {
		nominatimGeocoder = new NominatimGeocoder();
		nominatimGeocoder.setNominatimUrl("https://nominatim.openstreetmap.org/search.php");
		nominatimGeocoder.setResultLimit(6);
		
		googleGeocoder = new GoogleGeocoder();
		googleGeocoder.setApiKey("");
	
		mobiMongoDb = MongoClients.create("mongodb://localhost:27017").getDatabase("realtime_db");
		deserializer = new JSONDeserializer<GeocoderResults>().use(null, GeocoderResults.class);
		serializer = new JSONSerializer();
	}
	
	@Override
	public GeocoderResults geocode(String address, Envelope bbox) {
		address = address.toLowerCase();

		// Try MongoDB
		MongoCollection<Document> collection = getGeoCollection(address);
		Document regexQuery = new Document("address", new Document("$regex", address));
		Document mongoResult = collection.find(regexQuery).iterator().tryNext();
		if (mongoResult != null) {
			return (GeocoderResults)deserializer.deserialize(mongoResult.getString("value"));
		}
		
		// Try Nominatim
		GeocoderResults result = nominatimGeocoder.geocode(address, bbox);
		if (result.getCount() > 0) {
			cacheResult(collection, address, result);
			return result;
		}
		
		// Try Google
		result = googleGeocoder.geocode(address, bbox);
		if (result.getCount() > 0) {
			cacheResult(collection, address, result);
			return result;
		}
		
		// Return empty value
		cacheResult(collection, address, result);
		return result;
	}
	
	private void cacheResult(MongoCollection<Document> collection, String address, GeocoderResults result) {
		Document document = new Document();
		document.put("address", address);
		document.put("value", serializer.deepSerialize(result));
		document.put("updatedAt", new Date().getTime());
		collection.insertOne(document);
	}

	private MongoCollection<Document> getGeoCollection(String address) {
		String[] addrSplit = address.split(",");
		String collectionName = addrSplit.length < 4 ? "geo-generic" : 
			Arrays.stream(addrSplit).skip(addrSplit.length - 3).reduce("geo", (acc, value) -> acc + "-" + value.replace(" ", ""));

		// Verify if the collection already exist
		Boolean collectionExist = false;
		for (String cName : mobiMongoDb.listCollectionNames()) {
			if (cName == collectionName) {
				collectionExist = true;
				break;
			}
		}

		MongoCollection<Document> collection = mobiMongoDb.getCollection(collectionName);
		// If is a new collection create a index for the address
		if (!collectionExist)
			collection.createIndex(Indexes.ascending("address"));

		return collection;
	}
}
