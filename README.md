# Industrial Android Vision Systems

A professional-grade Android application that transforms any Android smartphone into a powerful industrial vision inspection system. Built with cutting-edge multi-agent AI technologies, deep learning, and LLM integration.

## Features

### Core Vision Capabilities
- **Real-time Defect Detection** - Deep learning powered detection of scratches, dents, cracks, corrosion, and more
- **OCR Text Recognition** - Multi-language text extraction, serial number verification, date code parsing
- **Dimensional Measurement** - Precise measurements with calibration support and tolerance checking
- **Barcode/QR Scanning** - High-speed scanning with ML Kit integration
- **Color Analysis** - Delta-E color matching and analysis
- **Surface Inspection** - Detection of surface defects and anomalies
- **Assembly Verification** - Component presence and positioning verification

### Multi-Agent AI System
- **Distributed Agent Architecture** - Specialized AI agents for different inspection tasks
- **Agent Orchestrator** - Intelligent coordination of multiple agents with parallel processing
- **Inter-agent Communication** - Message-based communication with priority handling
- **Dynamic Pipeline Construction** - Auto-generated inspection pipelines based on profiles

### LLM Integration
- **Anthropic Claude** - Primary provider for intelligent analysis
- **Vision-Language Models** - Image analysis with natural language understanding
- **Root Cause Analysis** - AI-powered defect root cause identification
- **Natural Language Chat** - Interactive quality control assistant
- **Intelligent Insights** - Automated recommendations and predictions

### Professional Features
- **Inspection Profiles** - Configurable templates for different inspection types
- **Real-time Processing** - Live camera feed with instant defect detection
- **Analytics Dashboard** - Comprehensive statistics and trend analysis
- **Cloud Sync** - Synchronization with cloud services
- **Export & Reporting** - PDF, CSV, JSON report generation
- **Calibration Tools** - Camera and measurement calibration

## Architecture

```
industrial-vision-systems/
├── app/                    # Main application module
├── core/
│   ├── common/            # Common utilities and extensions
│   ├── data/              # Data layer (Room, repositories)
│   ├── domain/            # Domain models and business logic
│   ├── network/           # Network and cloud sync
│   └── ui/                # Common UI components
├── feature/
│   ├── camera/            # Camera capture feature
│   ├── inspection/        # Inspection workflow
│   ├── agents/            # AI agents management
│   ├── analytics/         # Analytics dashboard
│   └── settings/          # Settings and configuration
├── vision/
│   ├── processing/        # Vision pipeline and camera
│   └── ml/                # ML models and processors
└── ai/
    ├── agents/            # Multi-agent system
    └── llm/               # LLM integration
```

## Technology Stack

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture + MVVM
- **Dependency Injection**: Hilt
- **Database**: Room
- **Camera**: CameraX
- **ML**: TensorFlow Lite, ML Kit
- **Network**: Retrofit, OkHttp
- **Async**: Kotlin Coroutines & Flow

## AI Agents

| Agent | Type | Description |
|-------|------|-------------|
| Defect Detector | Vision | Deep learning defect detection |
| Quality Assessor | Analysis | Multi-factor quality scoring |
| OCR Reader | Recognition | Text recognition and verification |
| Barcode Scanner | Recognition | QR/Barcode detection and decoding |
| Dimensional Analyzer | Measurement | Precision measurement |
| Color Analyzer | Analysis | Color matching with Delta-E |
| Surface Inspector | Vision | Surface quality inspection |
| Anomaly Detector | Analysis | Unsupervised anomaly detection |
| Insight Generator | LLM | AI-powered insights |
| Recommendation Engine | LLM | Actionable recommendations |
| Root Cause Analyzer | LLM | Defect root cause analysis |

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 26+ (Android 8.0)
- Kotlin 1.9.22+

### Installation

1. Clone the repository
```bash
git clone https://github.com/auxcorp/AndroidVisionSystems.git
```

2. Open in Android Studio

3. Sync Gradle files

4. Build and run on device

### Configuration

1. Set up API keys for LLM providers in `local.properties`:
```properties
ANTHROPIC_API_KEY=your_api_key
```

2. Configure cloud sync endpoint (optional):
```kotlin
cloudSyncService.configure(
    baseUrl = "https://your-api.com",
    apiKey = "your_api_key"
)
```

## Usage

### Basic Inspection
1. Launch the app
2. Select an inspection profile
3. Point camera at the target
4. Tap "Capture" or use "Live Inspection"
5. Review results and AI insights

### Creating Custom Profiles
1. Go to Settings > Profiles
2. Create new profile
3. Select enabled modules
4. Configure thresholds
5. Save and use

### Live Inspection Mode
1. Select "Live Inspection" from dashboard
2. Camera feed with real-time detection overlays
3. Defects highlighted in real-time
4. Statistics updated continuously

## Development

### Building
```bash
./gradlew assembleDebug
```

### Testing
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Code Style
The project follows Kotlin official style guide with ktlint.

## License

MIT License - see LICENSE file for details.

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## Support

For support and questions:
- Create an issue on GitHub
- Contact: support@industrialvision.ai

---

Built with AI-powered technologies by Industrial Vision Systems Team
