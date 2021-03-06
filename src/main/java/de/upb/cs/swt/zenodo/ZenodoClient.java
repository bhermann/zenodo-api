package de.upb.cs.swt.zenodo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by benhermann on 31.05.17.
 */
public class ZenodoClient implements ZenodoAPI {

	private static final String productionToken = "tokengoeshere";
	private static final String sandboxToken = "tokengoeshere";

	private static final String sandboxURL = "https://sandbox.zenodo.org/";
	private static final String productionURL = "https://zenodo.org/";

	private abstract class MyObjectMapper implements ObjectMapper {
		public abstract <T> T readValue(String value, TypeReference<T> valueType);
	}

	private final MyObjectMapper objectMapper;

	private String baseURL;
	private String token;

	public ZenodoClient(String baseURL, String token) {
		this.baseURL = baseURL;
		this.token = token;

		objectMapper = new MyObjectMapper() {
			final ISO8601DateFormat dateFormat = new ISO8601DateFormat() {
				@Override
				public Date parse(String source) throws ParseException {
					if (!source.endsWith("+0000") && !source.endsWith("+00:00"))
						source = source + "+0000";
					return super.parse(source);
				}
			};
			private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

			{
				jacksonObjectMapper.setDateFormat(dateFormat);
				jacksonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			}

			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return jacksonObjectMapper.readValue(value, valueType);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			public <T> T readValue(String value, TypeReference<T> valueType) {
				try {
					return jacksonObjectMapper.readValue(value, valueType);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value) {
				try {
					return jacksonObjectMapper.writeValueAsString(value);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		};
		Unirest.setObjectMapper(objectMapper);
	}

	public boolean test() {
		GetRequest request = prepareGetRequest(baseURL + API.Deposit.Depositions);
		try {
			HttpResponse<String> response = request.asString();
			if (response.getStatus() == 200)
				return true;
		} catch (UnirestException e) {
		}

		return false;
	}

	public Deposition getDeposition(Integer id) {
		GetRequest request = prepareGetRequest(baseURL + API.Deposit.Entity);
		request.routeParam("id", id.toString());
		try {
			HttpResponse<Deposition> response = request.asObject(Deposition.class);
			return response.getBody();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Deposition> getDepositions() {
		ArrayList<Deposition> result = new ArrayList<Deposition>();
		GetRequest request = prepareGetRequest(baseURL + API.Deposit.Depositions);
		try {
			ArrayList<Deposition> response = fromJSON(new TypeReference<ArrayList<Deposition>>() {
			}, request.asString().getBody());
			return response;
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Deposition updateDeposition(Deposition deposition) {
		HttpRequestWithBody request = preparePutRequest(baseURL + API.Deposit.Entity);
		request.routeParam("id", deposition.id.toString());
		try {
			HttpResponse<Deposition> response = request.asObject(Deposition.class);
			return response.getBody();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void deleteDeposition(Integer id) {
		HttpRequestWithBody request = prepareDeleteRequest(baseURL + API.Deposit.Entity);
		request.routeParam("id", id.toString());
		try {
			HttpResponse<String> response = request.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
	}

	private <T> T fromJSON(final TypeReference<T> type, final String jsonPacket) {
		T data = null;
		try {
			data = objectMapper.readValue(jsonPacket, type);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;
	}

	public Deposition createDeposition() throws UnsupportedOperationException, IOException {
		return createDeposition(null);
	}

	public Deposition createDeposition(final Metadata m) throws UnsupportedOperationException, IOException {
		HttpRequestWithBody post = preparePostRequest(baseURL + API.Deposit.Depositions);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		String data = "{}";
		if (m != null)
			data = objectMapper.writeValue(new Object() {
				public Metadata metadata = m;
			});

		RequestBodyEntity completePost = post.body(data);
        completePost.getEntity().writeTo(bytes);
        System.out.println(bytes.toString());
		try {
			HttpResponse<Deposition> response = completePost.asObject(Deposition.class);
			return response.getBody();

		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Created by agupta on 19.11.18. to get the list of files for a particular
	 * deposition
	 */
	public List<DepositionFile> getFiles(Integer depositionId) {
		GetRequest request = prepareGetRequest(baseURL + API.Deposit.Files);
		request.routeParam("id", depositionId.toString());
		try {
			ArrayList<DepositionFile> response = fromJSON(new TypeReference<ArrayList<DepositionFile>>() {
			}, request.asString().getBody());
			return response;
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;

	}

	public DepositionFile uploadFile(final FileMetadata f, Integer depositionId) throws UnsupportedOperationException, IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		HttpRequestWithBody post = preparePostFileRequest(baseURL + API.Deposit.Files).routeParam("id",
				depositionId.toString());
		String data = "{}";
		data = objectMapper.writeValue(new Object() {
			public FileMetadata files = f;
		});
		RequestBodyEntity completePost = post.body(data);
		try {
			completePost.getEntity().writeTo(bytes);
			System.out.println(bytes.toString());
			HttpResponse<DepositionFile> response = completePost.asObject(DepositionFile.class);
			System.out.println(response.getStatus() + " " + response.getStatusText() + response.getBody().toString());
			return response.getBody();

		} catch (UnirestException e) {
			e.printStackTrace();

		}
       return null;
	}

	public boolean publish(Integer id) {
		HttpRequestWithBody post = preparePostRequest(baseURL + API.Deposit.Publish).routeParam("id", id.toString());

		try {
			final HttpResponse<String> response = post.asString();
			if (response.getStatus() == 202)
				return true;
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean discard(Integer id) {
		HttpRequestWithBody post = preparePostRequest(baseURL + API.Deposit.Discard).routeParam("id", id.toString());

		try {
			final HttpResponse<String> response = post.asString();
			if (response.getStatus() == 201)
				return true;
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return false;
	}

	private HttpRequestWithBody preparePostRequest(String url) {
		return Unirest.post(url).header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
	}

	private HttpRequestWithBody preparePostFileRequest(String url) {
		return Unirest.post(url).header("Content-Type", "application/octet-stream").header("Authorization",
				"Bearer " + token);
	}

	private GetRequest prepareGetRequest(String url) {
		return Unirest.get(url).header("Authorization", "Bearer " + token);
	}

	private HttpRequestWithBody preparePutRequest(String url) {
		return Unirest.put(url).header("Content-Type", "application/json").header("Authorization", "Bearer" + token);
	}

	private HttpRequestWithBody prepareDeleteRequest(String url) {
		return Unirest.delete(url).header("Content-Type", "application/json").header("Authorization",
				"Bearer " + token);
	}

//	public static void main(String[] args) throws UnsupportedOperationException, IOException, UnirestException {
//		ZenodoClient client = new ZenodoClient(sandboxURL, sandboxToken);
//		System.out.println(client.test());
//
////		 Metadata firstTry = new Metadata(Metadata.UploadType.DATASET,
////		 new Date(),
////		 "API test",
////		 "API test",
////		 "1.0",
////		 Metadata.AccessRight.CLOSED);
////
////		 Deposition deposition = client.createDeposition(firstTry);
//
//		HttpResponse<JsonNode> jsonResponse = Unirest.post("https://sandbox.zenodo.org/"+API.Deposit.Files).routeParam("id", Integer.toString(252680))
//         		   .header("Authorization", "Bearer "+ sandboxToken)
//				  .header("accept", "application/json")
//				  .field("filename", "archive.zip")
//				  .field("file", new File("/home/ankur/SHK/zenodo/archive.zip"))
//				  .asJson();
//		System.out.println(jsonResponse.getStatus());
////		FileMetadata firstFile = new FileMetadata(new File("/home/ankur/SHK/zenodo/archive.zip"));
////		DepositionFile newFile = client.uploadFile(firstFile,252119);
////		System.out.println("File Uploaded " + newFile.id + " " + newFile.filename);
//		List<Deposition> depositions = client.getDepositions();
//		for (Deposition d : depositions)
//			System.out.println(d.title + " " + d.created + " " + d.id);
//
//		List<DepositionFile> files = client.getFiles(252123);
//		for (DepositionFile f : files) {
//			System.out.println(f.filename + " " + f.id + " " + f.filesize + " " + f.links.download);
//		}
//
//
//
//
//	}
}
