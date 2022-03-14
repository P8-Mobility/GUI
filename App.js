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
    Text,
    useColorScheme,
    View,
} from 'react-native';

const Header = ({title}): Node => {
    const isDarkMode = useColorScheme() === 'dark';
    return (
        <View style={styles.header}>
            <Text
                style={[
                    styles.title,
                    {
                        color: isDarkMode ? "#fff" : "#000",
                    },
                ]}>
                {title}
            </Text>
        </View>
    );
};

const App: () => Node = () => {
    const isDarkMode = useColorScheme() === 'dark';
    let [feedback: string, setFeedback] = useState("Press start to start recording");
    let [result: boolean, setResult] = useState(false);

    return (
        <SafeAreaView style={styles.container}>
            <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'}/>
            <Header title={"App"}/>
            <View
                style={{
                    color: isDarkMode ? "#fff" : "#000",
                    flex: 1,
                }}>
                <View style={{
                    flexDirection: 'row',
                    justifyContent: "center",
                    alignContent: "center",
                    padding: 10,
                }}>
                    <Text style={styles.word}>"Pære"</Text>
                    <View style={{
                        justifyContent: "center",
                        alignContent: "center",
                    }}>
                        <Button title={"Play"} style={{
                            alignSelf: "center",
                            textAlign: "center",
                        }}/>
                    </View>
                </View>
                <Text style={styles.text}>{feedback}</Text>
            </View>
            <View style={styles.buttonView}>
                <Button title={"Start"} onPress={() => {setFeedback("No")}}/>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    header: {
        paddingVertical: 10,
        alignSelf: "stretch",
        backgroundColor: "#8a2be2"
    },
    title: {
        fontSize: 24,
        fontWeight: '600',
        alignSelf: "center",
    },
    text: {
        padding: 20,
        fontSize: 24,
        alignSelf: "center"
    },
    buttonView: {
        width: "100%",
        alignSelf: "center",
        padding: 50,
    },
    word: {
        fontSize: 48,
        alignSelf: "center",
        padding: 10,
    }
});

export default App;
