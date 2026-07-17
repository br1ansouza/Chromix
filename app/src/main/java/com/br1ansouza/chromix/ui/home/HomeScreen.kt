package com.br1ansouza.chromix.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.br1ansouza.chromix.R

@Composable
fun HomeScreen(
    currentLevel: Int,
    onStart: () -> Unit,
    onOpenLevels: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF23232B),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_chromix),
            contentDescription = "Chromix",
            modifier = Modifier.size(240.dp),
        )

        // Listras da bandeira do RS.
        Row(modifier = Modifier.padding(top = 20.dp)) {
            listOf(
                Color(0xFF00963F),
                Color(0xFFDF2A33),
                Color(0xFFECBE13),
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(color, RoundedCornerShape(2.dp)),
                )
            }
        }

        Text(
            text = "Um puzzle tri gaúcho",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = "Iniciar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "Nível $currentLevel",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 12.dp),
        )

        TextButton(onClick = onOpenLevels, modifier = Modifier.padding(top = 4.dp)) {
            Text("Escolher nível", color = Color.White.copy(alpha = 0.7f))
        }
    }
}
