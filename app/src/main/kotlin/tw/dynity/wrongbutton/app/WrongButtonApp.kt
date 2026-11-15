package tw.dynity.wrongbutton.app

import android.app.Application

class WrongButtonApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
