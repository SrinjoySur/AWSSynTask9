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
import java.util.Optional;
import java.util.function.Function;

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

	public Map<String, Object> handleRequest(Map<String , Object> event, Context context) {
		System.out.println("Handling request for /weather endpoint");
		String path = (String) event.getOrDefault("rawPath", event.get("path"));
		Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
		Map<String, Object> http = requestContext != null ? (Map<String, Object>) requestContext.get("http") : null;
		String method = http != null ? (String) http.get("method") : (String) event.get("httpMethod");

		if ("/weather".equals(path) && "GET".equalsIgnoreCase(method)) {
			return createWeatherResponse();
		} else {
			return createErrorResponse("Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + method);
		}
	}
	private Map<String, Object> createWeatherResponse() {
		Map<String, Object> body = new HashMap<>();
		body.put("latitude", 50.4375);
		body.put("longitude", 30.5);
		body.put("generationtime_ms", 0.025033950805664062);
		body.put("utc_offset_seconds", 7200);
		body.put("timezone", "Europe/Kiev");
		body.put("timezone_abbreviation", "EET");
		body.put("elevation", 188.0);

		body.put("hourly_units", Map.of(
				"time", "iso8601",
				"temperature_2m", "°C",
				"relative_humidity_2m", "%",
				"wind_speed_10m", "km/h"
		));

		body.put("hourly", Map.of(
				"time", new String[]{"2023-12-04T00:00", "2023-12-04T01:00", "2023-12-04T02:00", "..."},
				"temperature_2m", new Object[]{-2.4, -2.8, -3.2, "..."},
				"relative_humidity_2m", new Object[]{84, 85, 87, "..."},
				"wind_speed_10m", new Object[]{7.6, 6.8, 5.6, "..."}
		));

		body.put("current_units", Map.of(
				"time", "iso8601",
				"interval", "seconds",
				"temperature_2m", "°C",
				"wind_speed_10m", "km/h"
		));

		body.put("current", Map.of(
				"time", "2023-12-04T07:00",
				"interval", 900,
				"temperature_2m", 0.2,
				"wind_speed_10m", 10.0
		));

		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 200);
		response.put("headers", Map.of("Content-Type", "application/json"));
		response.put("body", body);
		response.put("isBase64Encoded", false);

		return response;
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
