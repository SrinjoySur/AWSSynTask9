package com.task09;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@LambdaHandler(
    lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "weather_sdk",
		libraries = {"lib/weather-sdk-1.0.0.jar"},
		runtime = DeploymentRuntime.JAVA11,
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<Map<String , Object>, Map<String, Object>> {
	private static final String OPEN_METEO_API_URL = "https://api.open-meteo.com/v1/forecast";

	public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
		System.out.println("Handling request for /weather endpoint");

		String path = (String) event.getOrDefault("rawPath", event.get("path"));
		String method = (String) event.getOrDefault("httpMethod", "GET");

		if ("/weather".equals(path) && "GET".equalsIgnoreCase(method)) {
			return fetchWeatherData();
		} else {
			return createErrorResponse("Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + method);
		}
	}

	private Map<String, Object> fetchWeatherData() {
		try {
			// Configure the HTTP request to Open-Meteo API
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(OPEN_METEO_API_URL + "?latitude=50.4375&longitude=30.5&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m"))
					.GET()
					.build();

			// Send the HTTP request and get the response
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			// Parse and return the response
			Map<String, Object> result = new HashMap<>();
			result.put("statusCode", 200);
			result.put("headers", Map.of("Content-Type", "application/json"));
			result.put("body", response.body());
			result.put("isBase64Encoded", false);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return createErrorResponse("Failed to fetch weather data: " + e.getMessage());
		}
	}

	private Map<String, Object> createErrorResponse(String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 400);
		response.put("headers", Map.of("Content-Type", "application/json"));
		response.put("body", String.format("{\"statusCode\": %d, \"message\": \"%s\"}", 400, message));
		response.put("isBase64Encoded", false);
		return response;
	}
}
