package com.example.camera.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ButtonTiny(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.padding(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme
                .typography.bodyMedium
                .copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Preview
@Composable
fun ButtonPreview() {
    ButtonTiny(
        onClick = {},
        text = "Halo",
        modifier = Modifier
    )
}