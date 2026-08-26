package org.telegram.messenger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.text.TextUtils;

import org.telegram.messenger.voip.VoIPService;

/** Read-only VoIP environment snapshot plus deliberate active-call route controls. */
public final class SingGramCallDiagnostics {

    private SingGramCallDiagnostics() {
    }

    public static Snapshot inspect() {
        Context context = ApplicationLoader.applicationContext;
        boolean microphonePresent = context != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
        boolean microphoneGranted = context != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
        AudioManager audioManager = context == null ? null : (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int outputBuffer = -1;
        try {
            outputBuffer = AudioTrack.getMinBufferSize(48_000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        } catch (Throwable ignore) {
        }
        VoIPService service = VoIPService.getSharedInstance();
        boolean activeCall = service != null;
        int route = activeCall ? service.getCurrentAudioRoute() : -1;
        int callState = activeCall ? service.getCallState() : -1;
        String lastError = activeCall ? service.getLastError() : "";
        return new Snapshot(microphonePresent, microphoneGranted, outputBuffer, audioManager == null ? -1 : audioManager.getMode(), activeCall, route, callState, lastError);
    }

    public static boolean useEarpiece() {
        return setRoute(VoIPService.AUDIO_ROUTE_EARPIECE);
    }

    public static boolean useSpeaker() {
        return setRoute(VoIPService.AUDIO_ROUTE_SPEAKER);
    }

    private static boolean setRoute(int route) {
        VoIPService service = VoIPService.getSharedInstance();
        if (service == null) {
            return false;
        }
        service.setAudioRoute(route);
        return true;
    }

    public static final class Snapshot {
        public final boolean microphonePresent;
        public final boolean microphoneGranted;
        public final int outputBuffer;
        public final int audioMode;
        public final boolean activeCall;
        public final int route;
        public final int callState;
        public final String lastError;

        private Snapshot(boolean microphonePresent, boolean microphoneGranted, int outputBuffer, int audioMode, boolean activeCall, int route, int callState, String lastError) {
            this.microphonePresent = microphonePresent;
            this.microphoneGranted = microphoneGranted;
            this.outputBuffer = outputBuffer;
            this.audioMode = audioMode;
            this.activeCall = activeCall;
            this.route = route;
            this.callState = callState;
            this.lastError = lastError;
        }

        public String microphoneSummary() {
            if (!microphonePresent) {
                return "No microphone hardware";
            }
            return microphoneGranted ? "Permission granted" : "RECORD_AUDIO permission missing";
        }

        public String outputSummary() {
            return outputBuffer > 0 ? "48 kHz mono buffer: " + outputBuffer + " bytes" : "Audio output buffer unavailable";
        }

        public String routeSummary() {
            if (!activeCall) {
                return "No active call";
            }
            switch (route) {
                case VoIPService.AUDIO_ROUTE_EARPIECE:
                    return "Earpiece / wired headset";
                case VoIPService.AUDIO_ROUTE_SPEAKER:
                    return "Speaker";
                case VoIPService.AUDIO_ROUTE_BLUETOOTH:
                    return "Bluetooth";
                default:
                    return "Unknown";
            }
        }

        public String callSummary() {
            if (!activeCall) {
                return "No active call";
            }
            String state = "state=" + callState + ", audio mode=" + audioMode;
            return TextUtils.isEmpty(lastError) ? state : state + ", last error=" + lastError;
        }

        public String buildReport() {
            StringBuilder builder = new StringBuilder("SingGram call diagnostics\n");
            builder.append("microphone: ").append(microphoneSummary()).append('\n');
            builder.append("output: ").append(outputSummary()).append('\n');
            builder.append("route: ").append(routeSummary()).append('\n');
            builder.append("call: ").append(callSummary());
            return builder.toString();
        }
    }
}
