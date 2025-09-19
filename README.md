#Test-Chess

An Android implementation of a simple chess game, also known as Makruk. This project brings the classic strategy game to mobile devices with a modern UI design.

## 🎯 Project Overview

This Android application is a digital version of Makruk. The game features the same strategic depth as international chess but with simplified rules and unique piece movements.

This is a school project for shree pokhariya secondary school of 11 tech batch: 2082.As a student I'm learning android development and this is my 2nd major project.

## 📱 Features

- Authentic implementation of Nepali classical chess rules
- Modern Material Design UI with Kotlin
- Responsive game board with intuitive touch controls
- Light and dark theme support
- Comprehensive testing suite

## 🛠️ Technical Stack

- **Language**: Pure Kotlin
- **Framework**: Android Jetpack Compose
- **Architecture**: Modern Android architecture components
- **Testing**: JUnit, Android Instrumentation Tests

## 📁 Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/krishna/nepaliclassicalchess/
│   │   │   ├── ui/theme/     # Theme definitions
│   │   │   └── MainActivity.kt  # Main entry point
│   │   └── res/              # Resources (images, values)
│   └── test/                 # Unit tests
│   └── androidTest/          # Instrumentation tests
└── build.gradle.kts          # Build configuration
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug or later
- Kotlin 1.9+ 
- Android SDK API level 24+

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Krishna-Baral029/Test-Chess.git
   ```

2. Open the project in Android Studio

3. Build and run the application

### Building the Project

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

## 🎮 Game Rules

Makruk is played on an 8×8 uncheckered board with the following pieces:

- **King** (Khun): Moves one square in any direction
- **Queen** (Met): Moves any number of squares diagonally
- **Rook** (Ruea): Moves any number of squares orthogonally
- **Bishop** (Khon): Moves one square diagonally
- **Knight** (Ma): Moves in an L-shape (two squares in one direction, then one square perpendicular)
- **Pawn** (Bia): Moves one square forward, captures diagonally forward

## 🧪 Testing

The project includes both unit tests and instrumentation tests:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumentation tests
./gradlew connectedDebugAndroidTest
```

## 📝 Development

### Adding New Features

1. Create a new branch for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Implement your changes

3. Write tests for your code

4. Commit and push your changes:
   ```bash
   git add .
   git commit -m "Add your feature description"
   git push origin feature/your-feature-name
   ```

### Code Style

This project follows Kotlin coding conventions and uses:
- Null safety practices
- Extension functions where appropriate
- Coroutines for asynchronous operations

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Krishna Baral**

- GitHub: [@Krishna-Baral029](https://github.com/Krishna-Baral029)

## 🙏 Acknowledgments

- Inspired by traditional Nepali board games
- Built with modern Android development practices
- Special thanks to the Android developer community and our teachers at shree pokhariya secondary school for guidance

## 🔧 Troubleshooting

If you encounter any issues:

1. Ensure you're using the latest version of Android Studio
2. Check that all required SDK components are installed
3. Clean and rebuild the project:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

---

**Enjoy playing Nepali Classical Chess game and hope you like it 💖** ♟️

Directly download the apk file from: "apkfilesbykrish029.netlify.app"
