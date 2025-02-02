package org.opentripplanner.geocoder.google;

import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class GoogleGeocoder implements Geocoder {
	private static final Logger LOG = LoggerFactory.getLogger(GoogleGeocoder.class);
	
	private GoogleJsonDeserializer googleJsonDeserializer = new GoogleJsonDeserializer();
	private String apiKey;

	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }

	@Override
	public GeocoderResults geocode(String address, Envelope bbox) {
		String content = null;
		
		try {
			// make json request
			URL googleGeocoderUrl = getGoogleGeocoderUrl(address);
            URLConnection conn = googleGeocoderUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            
            StringBuilder sb = new StringBuilder(128);
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            content = sb.toString();

		} catch (IOException e) {
			LOG.error("Error parsing Google geocoder response", e);
			return noGeocoderResult("Error parsing geocoder response");
		}
		
        GoogleGeocoderResults googleGeocoderResults = googleJsonDeserializer.parseResults(content);
        
        List<GoogleGeocoderResult> googleResults = googleGeocoderResults.getResults();
        List<GeocoderResult> geocoderResults = new ArrayList<GeocoderResult>();
        for (GoogleGeocoderResult googleGeocoderResult : googleResults) {
			Geometry geometry = googleGeocoderResult.getGeometry();
			Location location = geometry.getLocation();
			Double lat = location.getLat();
			Double lng = location.getLng();
			
			String formattedAddress = googleGeocoderResult.getFormatted_address();
			
			GeocoderResult geocoderResult = new GeocoderResult(lat, lng, formattedAddress);
			geocoderResults.add(geocoderResult);
		}

		return new GeocoderResults(geocoderResults);
	}

	private GeocoderResults noGeocoderResult(String error) {
		return new GeocoderResults(error);
	}

	private URL getGoogleGeocoderUrl(String address) throws IOException {
		UriBuilder uriBuilder = UriBuilder.fromUri("http://maps.google.com/maps/api/geocode/json");
		uriBuilder.queryParam("sensor", false);
		uriBuilder.queryParam("address", address);
		uriBuilder.queryParam("key", apiKey);
		URI uri = uriBuilder.build();
		return new URL(uri.toString());
	}

}
