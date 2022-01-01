import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import api.PodcastManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


val httpClient = HttpClient(CIO) {
    install(UserAgent) {
        agent = "Lyssna"
    }
}

val podcastManager = PodcastManager()


@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()

    var listOfStreams = runBlocking { podcastManager.getPodcasts() }
    var playerPodcastImage: ImageBitmap by remember { mutableStateOf(useResource("podcast_image.png") {loadImageBitmap(it)}) }
    var playerPodcastTitle: String by remember { mutableStateOf("")}
    var audioSystem: MediaPlayer? by remember { mutableStateOf(null) }
    var isPaused: Boolean? by remember { mutableStateOf(null) }
    var playerPosition: Float by remember { mutableStateOf(0f) }
    var podcastLength: Long by remember { mutableStateOf(0L) }
    var rssPromptOpen: Boolean by remember { mutableStateOf(false) }


    Scaffold(bottomBar = {
        Column(modifier = Modifier.clickable(enabled = false) {}.fillMaxWidth().height(70.dp).background(Color.Gray)) {
            Row {
                Image(
                    bitmap = playerPodcastImage,
                    contentDescription = "Podcast image",
                    modifier = Modifier.width(Dp(72f)).height(Dp(72f))
                )
                Column {
                    Text(playerPodcastTitle)
                    if(audioSystem != null) {
                        Row {
                            Button(
                                onClick = {
                                    if(isPaused == true) {
                                        isPaused = false
                                        audioSystem?.play()
                                    } else {
                                        isPaused = true
                                        audioSystem?.pause()
                                    }
                                }) {
                                if(isPaused == true) {
                                    Text("Play")
                                } else {
                                    Text("Pause")
                                }
                            }
                            if(podcastLength != 0L) {
                                Slider(value = playerPosition, onValueChange = {
                                                                               audioSystem?.seek(javafx.util.Duration(it.toDouble()*1000))
                                                                                playerPosition = audioSystem?.currentTime?.toSeconds()?.toFloat()!!
                                }, valueRange=0f..podcastLength.toFloat())
                            }
                        }
                    }
                }
            }

        }
    }) {

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(ScrollState(0)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if(rssPromptOpen) {
                var rssState: String by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = {
                        rssPromptOpen = false
                    },
                    title = {
                        Text(text = "Add a RSS podcast feed")
                    },
                    text = {
                        TextField(
                            value = rssState,
                            onValueChange = { rssState = it }
                        )
                    },
                    confirmButton = {
                        Button(

                            onClick = {
                                rssPromptOpen = false
                                podcastManager.addRssUrl(rssState)
                                listOfStreams = runBlocking { podcastManager.getPodcasts() }
                            }) {
                            Text("Add URL")
                        }
                    },
                    dismissButton = {
                        Button(

                            onClick = {
                                rssPromptOpen = false
                            }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
            Button(
                onClick = {
                    rssPromptOpen = true
                }) {
                Text("Add Podcast")
            }
            Text(
                text = "Your Lyssna feed.",
                modifier = Modifier.padding(27.dp),
                style = MaterialTheme.typography.h5,
            )
            listOfStreams.forEach {
                Card(modifier = Modifier.padding(4.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    Column(Modifier.clickable { expanded = !expanded }.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.Top) {
                            var podcastImage: ImageBitmap by remember { mutableStateOf(useResource("podcast_image.png") {loadImageBitmap(it)}) }
                            coroutineScope.async {
                                val image = httpClient.use { client ->
                                    it.imageUrl?.let { it1 -> client.get<ByteArray>(it1) }
                                }
                                podcastImage = org.jetbrains.skia.Image.makeFromEncoded(image).toComposeImageBitmap()
                            }
                            Row {
                                Image(
                                    bitmap = podcastImage,
                                    contentDescription = "Podcast image",
                                    modifier = Modifier.width(Dp(72f)).height(Dp(72f))
                                )
                                Text(
                                    text = it.title,
                                    modifier = Modifier.padding(20.dp),
                                )
                            }
                            Button(
                                onClick = {
                                    audioSystem?.stop()
                                    audioSystem?.dispose()
                                    playerPodcastImage = podcastImage
                                    playerPodcastTitle = it.title
                                    val currentSystem = MediaPlayer(Media(it.audioUrl))
                                    audioSystem = currentSystem
                                    it.audioLength?.let { podcastLength = it }
                                    audioSystem?.play()
                                    audioSystem?.onReady = Runnable {
                                        playerPosition = 0f
                                    }
                                    audioSystem?.onPlaying = Runnable {
                                        isPaused = false
                                        coroutineScope.async {
                                            while (isPaused == false && currentSystem == audioSystem) {
                                                delay(1000L)
                                                playerPosition += 1
                                            }
                                        }
                                    }
                                }) {
                                Text("Play")
                            }
                        }

                        AnimatedVisibility(expanded) {
                            it.description?.let { it1 ->
                                Text(
                                    text = it1,
                                    modifier = Modifier.padding(20.dp),
                                )
                            }
                        }
                    }
                }

            }
            Text(modifier = Modifier.padding(30.dp), text = "")
        }
    }

}

fun main() = application {
    Platform.startup(Runnable {})
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lyssna",
        icon = painterResource("icon.png"),
        resizable = false,
         state = WindowState(
            size = DpSize(Dp(400f), Dp(625f))
        )
    ) {
        App()
    }
}
