import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SimpleBot extends ListenerAdapter {

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final AudioPlayer player = playerManager.createPlayer();
    private final TrackScheduler trackScheduler = new TrackScheduler(player);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("!")) return;

        String[] parts = event.getMessage().getContentRaw().substring(1).split(" ", 2);
        String command = parts[0];
        String content = parts.length > 1 ? parts[1] : "";

        switch (command.toLowerCase()) {
            case "play":
                loadAndPlay(event.getTextChannel(), content);
                break;
            case "pause":
                togglePause();
                break;
            case "skip":
                skip();
                break;
            case "next":
                nextTrack();
                break;
            case "identify":
                identify(event.getTextChannel());
                break;
        }
    }

    private void loadAndPlay(TextChannel channel, String url) {
        playerManager.loadItemOrdered(player, url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue: " + track.getInfo().title).queue();
                trackScheduler.queue(track);
            }
            // Handle other methods if desired.
        });
    }

    private void togglePause() {
        if (player.isPaused()) {
            player.setPaused(false);
        } else {
            player.setPaused(true);
        }
    }

    private void skip() {
        trackScheduler.nextTrack();
    }

    private void nextTrack() {
        trackScheduler.nextTrack();
    }

    private void identify(TextChannel textChannel) {
        AudioTrack currentTrack = trackScheduler.getCurrentTrack();
        if (currentTrack != null) {
            textChannel.sendMessage("Currently playing: " + currentTrack.getInfo().title).queue();
        } else {
            textChannel.sendMessage("No track is currently playing.").queue();
        }
    }

    public static void main(String[] args) throws Exception {
        JDABuilder.createDefault("YOUR_BOT_TOKEN")
            .addEventListeners(new SimpleBot())
            .build();
    }

    private class TrackScheduler implements AudioLoadResultHandler {
        private final AudioPlayer player;
        private final BlockingQueue<AudioTrack> queue;

        public TrackScheduler(AudioPlayer player) {
            this.player = player;
            this.queue = new LinkedBlockingQueue<>();
        }

        public void queue(AudioTrack track) {
            if (!player.startTrack(track, true)) {
                queue.offer(track);
            }
        }

        public void nextTrack() {
            player.startTrack(queue.poll(), false);
        }

        public AudioTrack getCurrentTrack() {
            return player.getPlayingTrack();
        }
    }
}
