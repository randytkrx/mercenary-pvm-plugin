package com.claneventhub;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
public class ClanEventHubClient
{
	private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient client;
	private final Gson gson;

	@Inject
	private ClanEventHubClient(OkHttpClient client, Gson gson)
	{
		this.client = client;
		this.gson = gson;
	}

	// --- Discovery ---

	public CompletableFuture<JsonObject> getActiveEvents(String apiBase)
	{
		return getJson(apiBase + "/active-events", null, null);
	}

	// --- Survivor ---

	public CompletableFuture<JsonObject> joinEvent(String apiBase, String eventId, String username)
	{
		JsonObject body = new JsonObject();
		body.addProperty("eventId", eventId);
		body.addProperty("username", username);
		return postJson(apiBase + "/surv/join", null, null, body);
	}

	public CompletableFuture<JsonObject> getEventState(String apiBase, String eventId, String sessionToken)
	{
		HttpUrl url = HttpUrl.parse(apiBase + "/surv/state").newBuilder()
			.addQueryParameter("eventId", eventId)
			.build();
		return getJson(url.toString(), sessionToken, null);
	}

	public CompletableFuture<JsonObject> getCountdown(String apiBase, String eventId)
	{
		HttpUrl url = HttpUrl.parse(apiBase + "/surv/countdown").newBuilder()
			.addQueryParameter("eventId", eventId)
			.build();
		return getJson(url.toString(), null, null);
	}

	public CompletableFuture<JsonObject> submitProof(String apiBase, String eventId, String sessionToken,
		String proofUrl, String notes)
	{
		JsonObject body = new JsonObject();
		body.addProperty("eventId", eventId);
		body.addProperty("proofUrl", proofUrl);
		body.addProperty("notes", notes);
		return postJson(apiBase + "/surv/submit", sessionToken, null, body);
	}

	public CompletableFuture<JsonObject> useLifeline(String apiBase, String eventId, String sessionToken)
	{
		JsonObject body = new JsonObject();
		body.addProperty("eventId", eventId);
		return postJson(apiBase + "/surv/lifeline", sessionToken, null, body);
	}

	public CompletableFuture<JsonArray> getChat(String apiBase, String eventId)
	{
		HttpUrl url = HttpUrl.parse(apiBase + "/surv/chat").newBuilder()
			.addQueryParameter("eventId", eventId)
			.build();
		return getJsonArray(url.toString(), null, null);
	}

	public CompletableFuture<JsonObject> sendChat(String apiBase, String eventId, String sessionToken, String message)
	{
		JsonObject body = new JsonObject();
		body.addProperty("eventId", eventId);
		body.addProperty("message", message);
		return postJson(apiBase + "/surv/chat", sessionToken, null, body);
	}

	// --- Split tracker ---

	public CompletableFuture<JsonObject> submitSplit(String botUrl, String ign, long amount, String[] splitWith,
		String screenshotBase64, String apiKey)
	{
		CompletableFuture<JsonObject> future = new CompletableFuture<>();

		JsonObject body = new JsonObject();
		body.addProperty("ign", ign);
		body.addProperty("amount", amount);

		JsonArray splitArray = new JsonArray();
		for (String name : splitWith)
		{
			splitArray.add(name.trim());
		}
		body.add("splitWith", splitArray);

		if (screenshotBase64 != null && !screenshotBase64.isEmpty())
		{
			body.addProperty("screenshot", screenshotBase64);
		}

		Request.Builder reqBuilder = new Request.Builder()
			.url(botUrl + "/splitadd")
			.post(RequestBody.create(JSON_MEDIA, gson.toJson(body)));

		if (apiKey != null && !apiKey.isEmpty())
		{
			reqBuilder.header("Authorization", "Bearer " + apiKey);
		}

		enqueueJsonObject(reqBuilder.build(), future);
		return future;
	}

	// --- Bingo ---

	public CompletableFuture<JsonObject> submitBingoProof(String apiBase, String boardId, int tileId,
		String submittedBy, BufferedImage image, String sessionToken, String botSecret)
	{
		CompletableFuture<JsonObject> future = new CompletableFuture<>();

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageBytes = baos.toByteArray();

			MultipartBody body = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("image", "screenshot.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes))
				.addFormDataPart("tile_id", String.valueOf(tileId))
				.addFormDataPart("submittedBy", submittedBy)
				.build();

			Request.Builder reqBuilder = new Request.Builder()
				.url(apiBase + "/board/" + boardId + "/proof")
				.post(body);
			addAuthHeaders(reqBuilder, sessionToken, botSecret);

			enqueueJsonObject(reqBuilder.build(), future);
		}
		catch (IOException e)
		{
			future.completeExceptionally(e);
		}

		return future;
	}

	// --- Image upload ---

	public CompletableFuture<JsonObject> uploadImage(String apiBase, BufferedImage image,
		String sessionToken, String botSecret)
	{
		CompletableFuture<JsonObject> future = new CompletableFuture<>();

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageBytes = baos.toByteArray();

			MultipartBody body = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("image", "screenshot.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes))
				.build();

			Request.Builder reqBuilder = new Request.Builder()
				.url(apiBase + "/upload")
				.post(body);
			addAuthHeaders(reqBuilder, sessionToken, botSecret);

			enqueueJsonObject(reqBuilder.build(), future);
		}
		catch (IOException e)
		{
			future.completeExceptionally(e);
		}

		return future;
	}

	// --- Hall of Fame ---

	public CompletableFuture<JsonObject> addDrop(String apiBase, String title, String player,
		String imageUrl, String botSecret)
	{
		JsonObject body = new JsonObject();
		body.addProperty("title", title);
		body.addProperty("player", player);
		body.addProperty("image", imageUrl);
		body.addProperty("category", "drop");
		return postJson(apiBase + "/hall-of-fame", null, botSecret, body);
	}

	public CompletableFuture<JsonArray> getDrops(String apiBase, int limit)
	{
		HttpUrl url = HttpUrl.parse(apiBase + "/hall-of-fame").newBuilder()
			.addQueryParameter("limit", String.valueOf(limit))
			.build();
		return getJsonArray(url.toString(), null, null);
	}

	// --- Internal HTTP helpers ---

	private void enqueueJsonObject(Request request, CompletableFuture<JsonObject> future)
	{
		client.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Request failed: {}", request.url(), e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					String body = response.body().string();
					if (response.isSuccessful())
					{
						if (body.trim().startsWith("<"))
						{
							log.warn("Got HTML instead of JSON from {}", request.url());
							future.completeExceptionally(
								new IOException("Server returned HTML instead of JSON - check your API URL"));
							return;
						}
						JsonElement parsed = new JsonParser().parse(body);
						future.complete(parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject());
					}
					else
					{
						log.warn("Request {} returned {} - {}", request.url(), response.code(), body);
						future.completeExceptionally(new IOException("HTTP " + response.code() + ": " + body));
					}
				}
			}
		});
	}

	private CompletableFuture<JsonObject> getJson(String url, String sessionToken, String botSecret)
	{
		CompletableFuture<JsonObject> future = new CompletableFuture<>();
		Request.Builder reqBuilder = new Request.Builder().url(url).get();
		addAuthHeaders(reqBuilder, sessionToken, botSecret);
		enqueueJsonObject(reqBuilder.build(), future);
		return future;
	}

	private CompletableFuture<JsonArray> getJsonArray(String url, String sessionToken, String botSecret)
	{
		CompletableFuture<JsonArray> future = new CompletableFuture<>();

		Request.Builder reqBuilder = new Request.Builder().url(url).get();
		addAuthHeaders(reqBuilder, sessionToken, botSecret);

		client.newCall(reqBuilder.build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("GET {} failed", url, e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					String body = response.body().string();
					if (response.isSuccessful())
					{
						JsonElement parsed = new JsonParser().parse(body);
						future.complete(parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray());
					}
					else
					{
						log.warn("GET {} returned {} - {}", url, response.code(), body);
						future.completeExceptionally(new IOException("HTTP " + response.code() + ": " + body));
					}
				}
			}
		});

		return future;
	}

	private CompletableFuture<JsonObject> postJson(String url, String sessionToken, String botSecret, JsonObject body)
	{
		CompletableFuture<JsonObject> future = new CompletableFuture<>();
		Request.Builder reqBuilder = new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON_MEDIA, gson.toJson(body)));
		addAuthHeaders(reqBuilder, sessionToken, botSecret);
		enqueueJsonObject(reqBuilder.build(), future);
		return future;
	}

	private void addAuthHeaders(Request.Builder builder, String sessionToken, String botSecret)
	{
		if (sessionToken != null && !sessionToken.isEmpty())
		{
			builder.header("x-session-token", sessionToken);
		}
		if (botSecret != null && !botSecret.isEmpty())
		{
			builder.header("x-bot-secret", botSecret);
		}
	}
}
