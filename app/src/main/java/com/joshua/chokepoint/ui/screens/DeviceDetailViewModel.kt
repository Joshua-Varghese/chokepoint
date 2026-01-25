import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.model.SensorData
import com.joshua.chokepoint.data.mqtt.MqttRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceDetailViewModel(private val repository: MqttRepository) : ViewModel() {

    // Directly expose the repository's sensor data flow
    val sensorData: StateFlow<SensorData> = repository.sensorData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SensorData())

    val isConnected: StateFlow<Boolean> = repository.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun connect() {
        // Repository handles connection check internally
        repository.connect()
    }
}
