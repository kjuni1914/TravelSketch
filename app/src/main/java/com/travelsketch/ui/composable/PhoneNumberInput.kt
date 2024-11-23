package com.travelsketch.ui.composable
// 준희 추가
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberInput(
    phoneNumber: PhoneNumberState,
    onPhoneNumberChange: (PhoneNumberState) -> Unit
) {
    val focusRequester2: FocusRequester = remember { FocusRequester() }
    val focusRequester3: FocusRequester = remember { FocusRequester() }
    val options = listOf("010", "011", "016", "017", "018", "019")
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(3f)
        ) {
            OutlinedTextField(
                value = phoneNumber.part1,
                onValueChange = { },
                label = { Text("Phone") },
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        onClick = {
                            onPhoneNumberChange(phoneNumber.copy(part1 = option))
                            expanded = false
                            focusRequester2.requestFocus()
                        }
                    )
                }
            }
        }

//        Divider(
//            modifier = Modifier
//                .weight(0.5f)
//                .height(1.dp)
//                .align(Alignment.CenterVertically),
//            color = Color.Gray
//        )

        OutlinedTextField(
            value = phoneNumber.part2,
            onValueChange = {
                if (it.length <= 4) {
                    onPhoneNumberChange(phoneNumber.copy(part2 = it))
                    if (it.isEmpty()) focusRequester2.requestFocus()
                    if (it.length == 4) focusRequester3.requestFocus()
                }
            },
            label = { Text("") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(4f)
                .focusRequester(focusRequester2),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 18.sp)
        )

//        Divider(
//            modifier = Modifier
//                .weight(0.5f)
//                .height(1.dp)
//                .align(Alignment.CenterVertically),
//            color = Color.Gray
//        )


        OutlinedTextField(
            value = phoneNumber.part3,
            onValueChange = {
                if (it.length <= 4) {
                    onPhoneNumberChange(phoneNumber.copy(part3 = it))
                    if (it.isEmpty()) focusRequester2.requestFocus()
                }
            },
            label = { Text("") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(4f)
                .focusRequester(focusRequester3),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 18.sp)
        )
    }
}

data class PhoneNumberState(
    val part1: String = "",
    val part2: String = "",
    val part3: String = ""
) {
    fun fullNumber(): String = if (part1.isNotEmpty() && part2.isNotEmpty() && part3.isNotEmpty()) {
        "$part1$part2$part3"
    } else {
        ""
    }
}
