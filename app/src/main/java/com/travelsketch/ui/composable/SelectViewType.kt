import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.travelsketch.data.model.ViewType
import com.travelsketch.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun SelectViewType(
    loginViewModel: LoginViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Select your canvas view type",
            style = TextStyle(
                fontSize = 32.sp
            )
        )

        IconButton(
            onClick = {
                val userId = loginViewModel.currentUser()?.uid
                if (userId != null) {
                    loginViewModel.viewModelScope.launch {
                        loginViewModel.saveViewType(ViewType.MAP)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.LightGray, shape = RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Map View",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        IconButton(
            onClick = {
                val userId = loginViewModel.currentUser()?.uid
                if (userId != null) {
                    loginViewModel.viewModelScope.launch {
                        loginViewModel.saveViewType(ViewType.LIST)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Gray, shape = RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "List View",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}