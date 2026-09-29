package com.example.havenapp.api;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {

        if (retrofit == null) {

            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .addInterceptor(new Interceptor() {

                                @Override
                                public okhttp3.Response intercept(
                                        Chain chain
                                ) throws java.io.IOException {

                                    Request original =
                                            chain.request();

                                    Request request =
                                            original.newBuilder()

                                                    .addHeader(
                                                            "apikey",
                                                            SupabaseConfig.API_KEY
                                                    )

                                                    .build();

                                    return chain.proceed(request);
                                }
                            })
                            .build();


            retrofit =
                    new Retrofit.Builder()

                            .baseUrl(
                                    SupabaseConfig.BASE_URL
                            )

                            .client(client)

                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )

                            .build();
        }

        return retrofit;
    }
}