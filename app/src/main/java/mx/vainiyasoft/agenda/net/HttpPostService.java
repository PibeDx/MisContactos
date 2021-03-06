package mx.vainiyasoft.agenda.net;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import mx.vainiyasoft.agenda.MainActivity;
import mx.vainiyasoft.agenda.R;
import mx.vainiyasoft.agenda.data.ContactOperations;
import mx.vainiyasoft.agenda.entity.JSONBean;
import mx.vainiyasoft.agenda.util.ApplicationContextProvider;
import mx.vainiyasoft.agenda.util.NotificationController;

/**
 * Created by alejandro on 6/16/14.
 */
public class HttpPostService extends IntentService {

    public final int NOTIFICATION_ID = HttpServiceBroker.SYNC_SERVICE_NOTIFICATION_ID + HttpServiceBroker.HTTP_POST_METHOD;
    private final ObjectMapper mapper;

    public HttpPostService() {
        super("HttpPostService");
        mapper = MainActivity.getObjectMapper();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(intent.getStringExtra("url"));
        httpPost.addHeader("Content-Type", "application/json");
        try {
            JSONBean bean = intent.getParcelableExtra("bean");
            String data = mapper.writeValueAsString(bean);
            StringEntity entity = new StringEntity(data);
            httpPost.setEntity(entity);
            HttpResponse resp = client.execute(httpPost);
            String respStr = EntityUtils.toString(resp.getEntity());
            processResponse(intent, respStr, bean);
        } catch (IOException ex) {
            Log.e("HttpPostService", ex.getLocalizedMessage(), ex);
        }
    }


    private void processResponse(Intent intent, String respStr, JSONBean bean) {
        try {
            JsonNode node = mapper.readTree(respStr);
            int serverId = node.path("serverId").asInt();
            // TODO: Eliminar Log al finalizar las pruebas
            Log.i("ServerID Recibido", String.valueOf(serverId));
            bean.setServerId(serverId);
            Intent resp_intent = new Intent(ContactOperations.FILTER_NAME);
            resp_intent.putExtra("operacion", ContactOperations.ACCION_ACTUALIZAR_CONTACTO);
            resp_intent.putExtra("datos", bean);
            Context ctx = ApplicationContextProvider.getContext();
            ctx.sendBroadcast(resp_intent);
            notificarRespuesta(intent);
        } catch (IOException ex) {
            Log.e("HttpPostService", ex.getLocalizedMessage(), ex);
        }
    }

    private void notificarRespuesta(Intent intent) {
        int maxProgress = intent.getIntExtra("maxProgress", -1);
        int currentProgress = intent.getIntExtra("currentProgress", -1);
        NotificationController.notify(i18n(R.string.app_name),
                i18n(R.string.mesg_service_sync, "creados"), NOTIFICATION_ID, currentProgress,
                maxProgress);
    }

    private String i18n(int resourceId, Object... formatArgs) {
        Context ctx = ApplicationContextProvider.getContext();
        return ctx.getResources().getString(resourceId, formatArgs);
    }

}
