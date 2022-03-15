/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState} from 'react';
import type {Node} from 'react';
import {
    Button,
    SafeAreaView,
    StatusBar,
    StyleSheet,
    Text, TouchableOpacity,
    useColorScheme,
    View,
} from 'react-native';

const Sound = require('react-native-sound')

const Header = ({title}): Node => {
    const isDarkMode = useColorScheme() === 'dark';
    return (
        <View style={styles.header}>
            <Text
                style={[
                    styles.title,
                    {
                        color: isDarkMode ? "#fff" : "#fff",
                    },
                ]}>
                {title}
            </Text>
        </View>
    );
};

const colorScheme = {"primary": "dodgerblue", "secondary": "lavender"}

const App: () => Node = () => {
    const isDarkMode = useColorScheme() === 'dark';
    const sound = new Sound('paere.wav', Sound.MAIN_BUNDLE, (error) => {
        if (error) {
            console.log("Unable to load native pronunciation sample:", error.message);
        }
    })
    let [feedback: string, setFeedback] = useState("Press start to start recording");
    let [result: boolean, setResult] = useState(false);

    function playPronunciationSample(): void {
        if (sound) {
            sound.play(success => {
                if (!success) {
                    console.log("Unable to play native pronunciation sample")
                }
                sound.release()
            });
        } else {
            console.log("Unable to play native pronunciation sample")
        }
    }

    return (
        <SafeAreaView style={styles.container}>
            <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'}/>
            <Header title={"App"}/>
            <View style={styles.container}>
                <View style={styles.row}>
                    <Text style={styles.word}>"Pære"</Text>
                    <TouchableOpacity onPress={() => playPronunciationSample()} style={styles.buttonSmall}>
                        <Text style={styles.buttonText}>Listen</Text>
                    </TouchableOpacity>
                </View>
                <Text style={styles.feedback}>{feedback}</Text>
            </View>
            <TouchableOpacity onPress={() => setFeedback("No")} style={styles.button}>
                <Text style={styles.buttonText}>Start</Text>
            </TouchableOpacity>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    row: {
        flex: 1,
        flexDirection: 'row',
        justifyContent: "center",
        alignContent: "center",
        padding: 10,
    },
    header: {
        paddingVertical: 10,
        alignSelf: "stretch",
        backgroundColor: colorScheme.primary,
    },
    title: {
        fontSize: 32,
        fontWeight: '600',
        alignSelf: "center",
    },
    feedback: {
        padding: 20,
        fontSize: 24,
        alignSelf: "center",
        flex: 1,
    },
    button: {
        alignSelf: "center",
        backgroundColor: colorScheme.primary,
        borderRadius: 10,
        paddingVertical: 20,
        paddingHorizontal: 80,
        margin: 20,
        elevation: 5,
    },
    buttonText: {
        fontSize: 24,
        alignSelf: "center",
        color: "white",
    },
    buttonSmall: {
        alignSelf: "center",
        backgroundColor: colorScheme.primary,
        borderRadius: 10,
        paddingVertical: 10,
        paddingHorizontal: 10,
        elevation: 5,
    },
    word: {
        fontSize: 48,
        alignSelf: "center",
        padding: 10,
        marginEnd: 10,
        textDecorationLine: "underline",
    }
});

export default App;
